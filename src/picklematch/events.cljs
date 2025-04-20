(ns picklematch.events
  (:require
   [re-frame.core :as rf]
   [picklematch.firebase :as fb]
   [picklematch.rating :as rating]
   [clojure.string :as str]))

;; -- Initialize the app-db --
(rf/reg-event-db
 :initialize
 (fn [_ _]
   {:user nil
    :players {}
    :games []
    :selected-date (js/Date.)
    :log-of-rating-events []
    :loading? false}))

;; -- Sign in with Google --
(rf/reg-event-fx
 :sign-in-with-google
 (fn [{:keys [db]} _]
   {:db db
    :firebase/google-sign-in true}))

(rf/reg-fx
 :firebase/google-sign-in
 (fn [_]
   (fb/google-sign-in)))

;; -- Sign in with Facebook --
(rf/reg-event-fx
 :sign-in-with-facebook
 (fn [{:keys [db]} _]
   {:db db
    :firebase/facebook-sign-in true}))

(rf/reg-fx
 :firebase/facebook-sign-in
 (fn [_]
   (fb/facebook-sign-in)))

;; Once sign-in is successful, store user in DB & Firestore if new.
(rf/reg-event-fx
 :login-success
 (fn [{:keys [db]} [_ {:keys [uid email]}]]
   (fb/store-user-if-new! uid email)
   {:db (assoc db :user {:uid uid
                         :email email})
    :dispatch-n [
                [:load-user-details uid]
                [:load-all-users]
                ;; load all game dates
                [:load-all-game-dates]
                ;; also load today's games
                 [:load-games-for-date (-> (js/Date.) .toISOString (subs 0 10))]]}))

(rf/reg-event-fx
 :load-user-details
 (fn [{:keys [db]} [_ uid]]
   {:db (assoc db :loading? true)
    :firebase/load-user-details [uid true]}))  ;; pass as vector

;; A custom effect that calls firebase.cljs to load user doc
(rf/reg-fx
 :firebase/load-user-details
 (fn [[uid _dispatch?]]
   (fb/load-user-doc! uid
                      (fn [user-data]
                        (rf/dispatch [:user-details-loaded user-data]))
                      (fn [err]
                        (js/console.error "Error loading user doc:" err)
                        (rf/dispatch [:user-details-loaded nil])))))

(rf/reg-event-db
 :user-details-loaded
 (fn [db [_ user-data]]
   (if user-data
     (let [uid (:uid user-data)]
       (-> db
           (assoc :loading? false)
           (assoc-in [:players uid] (select-keys user-data [:rating :role]))
           (assoc-in [:user :role] (:role user-data))))
     (assoc db :loading? false))))

;; -- Toggle admin role
(rf/reg-event-fx
 :toggle-admin-role
 (fn [{:keys [db]} _]
   (let [uid (get-in db [:user :uid])
         current-role (get-in db [:user :role] "ordinary")
         new-role (if (= current-role "admin") "ordinary" "admin")]
     {:db (assoc-in db [:user :role] new-role)
      :firebase/update-user-role [uid new-role]})))

(rf/reg-fx
 :firebase/update-user-role
 (fn [[uid new-role]]
   (fb/update-user-role! uid new-role)))

;; -- Admin schedules a game
(rf/reg-event-fx
 :schedule-game
 (fn [{:keys [db]} [_ date-str time-str]]
   (when (and (seq (str/trim date-str))
              (seq (str/trim time-str)))
     {:db (assoc db :loading? true)
      :firebase/add-game [date-str time-str]})))

(rf/reg-fx
 :firebase/add-game
 (fn [[date-str time-str]]
   (fb/add-game! date-str time-str
                 (fn [game-id]
                   (rf/dispatch [:load-games-for-date date-str]))
                 (fn [err]
                   (js/console.error "Error scheduling game:" err)
                   (rf/dispatch [:games-loaded nil])))))

;; -- Load games for a selected date
(rf/reg-event-db
 :set-selected-date
 (fn [db [_ date-obj]]
   (assoc db :selected-date date-obj)))

(rf/reg-event-fx
 :load-games-for-date
 (fn [{:keys [db]} [_ date-str]]
   (js/console.log "Loading games for date:" date-str)
   {:db (assoc db :loading? true)
    :firebase/load-games-for-date [date-str true]}))

(rf/reg-fx
 :firebase/load-games-for-date
 (fn [[date-str _dispatch?]]
   (fb/load-games-for-date! date-str
                            (fn [games]
                              (rf/dispatch [:games-loaded games]))
                            (fn [err]
                              (js/console.error "Error loading games:" err)
                              (rf/dispatch [:games-loaded []])))))

(rf/reg-event-db
 :games-loaded
 (fn [db [_ games]]
   (-> db
       (assoc :loading? false)
       (assoc :games games))))

;; -- User registers for a game
(rf/reg-event-fx
 :register-for-game
 (fn [{:keys [db]} [_ game-id team-key]]
   (let [uid (get-in db [:user :uid])
         email (get-in db [:user :email])]
     (when uid
       {:db (assoc db :loading? true)
        :firebase/register-for-game [game-id team-key uid email]}))))

(rf/reg-fx
 :firebase/register-for-game
 (fn [[game-id team-key uid email]]
   (fb/register-for-game! game-id team-key uid email
                          (fn []
                            (js/console.log "Registration success for game" game-id)
                            (rf/dispatch [:reload-current-date-games]))
                          (fn [err]
                            (js/console.error "Error registering for game:" err)
                            (rf/dispatch [:games-loaded nil])))))

(rf/reg-event-fx
 :reload-current-date-games
 (fn [{:keys [db]} _]
   (let [date-obj (:selected-date db)
         date-str (.toISOString date-obj)]
     {:dispatch [:load-games-for-date (subs date-str 0 10)]})))

;; -- Submitting final game scores updates ratings
(rf/reg-event-fx
 :submit-game-result
 (fn [{:keys [db]} [_ game-id team1-score team2-score]]
   (let [games (:games db)
         game (some #(when (= (:id %) game-id) %) games)
         updated-game (assoc game
                             :team1-score team1-score
                             :team2-score team2-score)
         new-db (update db :games
                        (fn [gs]
                          (map (fn [g]
                                 (if (= (:id g) game-id)
                                   updated-game
                                   g))
                               gs)))]
     (let [[updated-ratings rating-event]
           (rating/recalculate-ratings new-db updated-game)]
       {:db (-> new-db
                (assoc :players updated-ratings)
                (update :log-of-rating-events conj rating-event))
        :firebase/store-game-score [updated-game]}))))

(rf/reg-fx
 :firebase/store-game-score
 (fn [[updated-game]]
   (fb/store-game-score! updated-game)))

(rf/reg-fx
 :firebase/load-all-users
 (fn [_]
   (fb/load-all-users!
    (fn [users-map]
      (rf/dispatch [:all-users-loaded users-map]))
    (fn [err]
      (js/console.error "Error loading all users" err)
      (rf/dispatch [:all-users-loaded {}])))))

(rf/reg-event-fx
 :load-all-users
 (fn [{:keys [db]} _]
   {:db db
    :firebase/load-all-users true}))

(rf/reg-event-db
 :all-users-loaded
 (fn [db [_ users-map]]
   (assoc db :players users-map)))


;; ---------------------------
;; Logout Event
;; ---------------------------
(rf/reg-event-fx
 :logout
 (fn [{:keys [db]} _]
   ;; Sign user out from Firebase, then set :user to nil
   {:db (assoc db :user nil)
    :firebase/logout true}))

;; A custom effect that calls firebase.cljs -> signOut
(rf/reg-fx
 :firebase/logout
 (fn [_]
   (fb/logout)))

;; ------------------------------
;; -- Load all game dates
;; ------------------------------
;; This event is triggered when the app loads to fetch all game dates
(rf/reg-event-fx
 :load-all-game-dates
 (fn [{:keys [db]} _]
   {:db (assoc db :loading? true)
    :firebase/load-all-game-dates true}))

(rf/reg-fx
 :firebase/load-all-game-dates
 (fn [_]
   ;; You must define this in firebase.cljs:
   ;; It fetches distinct dates from "games" collection
   (fb/load-all-game-dates!
    (fn [dates]
      (rf/dispatch [:all-game-dates-loaded dates]))
    (fn [err]
      (js/console.error "Error loading game dates:" err)
      (rf/dispatch [:all-game-dates-loaded #{}])))))

(rf/reg-event-db
 :all-game-dates-loaded
 (fn [db [_ dates]]
   (-> db
       (assoc :loading? false)
       (assoc :all-game-dates (set dates)))))  ;; store as a set of strings (e.g. #{"2025-04-17" ...})
