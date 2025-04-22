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
   (js/console.log "Initializing app-db")
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

;; ------------------------------
;; -- Send email link for sign-in
;; ------------------------------
;; This event is triggered when the user enters their email and clicks "Send Sign-In Link"

(rf/reg-event-fx
 :send-email-link
 (fn [{:keys [db]} [_ email]]
   (js/console.log "Sending email link to" email)
   {:db db
    :firebase/send-email-link email}))

(rf/reg-fx
 :firebase/send-email-link
 (fn [email]
   (picklematch.firebase/send-email-link! email)))


(rf/reg-event-fx
 :check-email-link
 (fn [{:keys [db]} _]
   ;; Called e.g. at app startup or on a special route
   {:db db
    :firebase/complete-email-link-sign-in true}))

(rf/reg-fx
 :firebase/complete-email-link-sign-in
 (fn [_]
   (picklematch.firebase/complete-email-link-sign-in!
    (.-search js/window.location))))
;; or pass (.-href js/window.location), depending on how you handle the URL

;; -- Register a new user --
(rf/reg-event-fx
 :register-with-email
 (fn [{:keys [db]} [_ email password]]
   {:db db
    :firebase/create-email-user [email password]}))

(rf/reg-fx
 :firebase/create-email-user
 (fn [[email password]]
   (fb/create-user-with-email!
    email
    password
    (fn [cred]
      (js/console.log "Created user with email/password" cred)
      ;; Optionally dispatch a success event, or rely on onAuthStateChanged
      (rf/dispatch [:login-success
                    {:uid (.-uid (.-user cred))
                     :email (.-email (.-user cred))}]))
    (fn [err]
      (js/console.error "Error creating user:" err)
      (let [code (.-code err)]  ;; e.g. "auth/email-already-in-use"
        (rf/dispatch [:auth-error code]))))))

;; -- Sign in existing user --
(rf/reg-event-fx
 :sign-in-with-email
 (fn [{:keys [db]} [_ email password]]
   {:db db
    :firebase/sign-in-email [email password]}))

(rf/reg-fx
 :firebase/sign-in-email
 (fn [[email password]]
   (fb/sign-in-with-email!
    email
    password
    (fn [cred]
      ;; On success:
      (js/console.log "Signed in with email/password" cred)
      (rf/dispatch [:login-success {:uid (.-uid (.-user cred))
                                    :email (.-email (.-user cred))}]))
    (fn [err]
      ;; On error:
      (js/console.error "Error signing in user:" err)
      (let [code (.-code err)]
        ;; Dispatch an event to store error in db:
        (rf/dispatch [:auth-error code]))))))


;; Now define an event to handle auth errors:

;; (rf/reg-event-db
;;  :show-auth-error
;;  (fn [db [_ code]]
;;    ;; We'll store a user-friendly string in db under :auth-error
;;    (assoc db :auth-error
;;           (case code
;;             "auth/email-already-in-use"
;;             "This email is already registered. Please sign in or use another email."

;;             "auth/weak-password"
;;             "Your password must be at least 6 characters long."

;;             "auth/invalid-login-credentials"
;;             ;; This code is not an official one from Firebase by default,
;;             ;; but your logs show it. Possibly "auth/wrong-password" or "auth/user-not-found".
;;             "Invalid email or password. Please try again."

;;             "auth/wrong-password"
;;             "Invalid email or password. Please try again."

;;             "auth/user-not-found"
;;             "No account exists for this email. Please register first."

;;             ;; Fallback
;;             (str "Something went wrong: " code)))))

(rf/reg-event-db
 :auth-error
 (fn [db [_ code]]
   (let [msg (case code
               "auth/email-already-in-use"   "That email is already registered."
               "auth/weak-password"          "Password must be at least 6 characters."
               "auth/invalid-login-credentials" "Invalid email/password. Please try again."
               "auth/user-not-found"         "No account found for that email."
               "auth/missing-password"       "You must enter a password."
               "Something went wrong.")]
      (js/console.log "Setting :auth-error in db to:" msg)
     (assoc db :auth-error msg))))
