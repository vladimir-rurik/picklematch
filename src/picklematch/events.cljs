(ns picklematch.events
  (:require
   [re-frame.core :as rf]
   [picklematch.firebase :as fb]
   [picklematch.rating :as rating]))

;; -- Initialize the app-db --
(rf/reg-event-db
 :initialize
 (fn [_ _]
   {:user                 nil
    :players              {}
    :games                []
    :current-session-date (js/Date.)
    :log-of-rating-events []
    :page                 :login}))

;; Change page
(rf/reg-event-db
 :set-page
 (fn [db [_ page-kw]]
   (assoc db :page page-kw)))

;; Google sign-in flow
(rf/reg-event-fx
 :sign-in-with-google
 (fn [{:keys [db]} _]
   {:db db
    :firebase/google-sign-in true}))

(rf/reg-fx
 :firebase/google-sign-in
 (fn [_]
   (fb/google-sign-in)))

;; Facebook sign-in flow
(rf/reg-event-fx
 :sign-in-with-facebook
 (fn [{:keys [db]} _]
   {:db db
    :firebase/facebook-sign-in true}))

(rf/reg-fx
 :firebase/facebook-sign-in
 (fn [_]
   (fb/facebook-sign-in)))

;; Called once auth is confirmed
(rf/reg-event-fx
 :login-success
 (fn [{:keys [db]} [_ {:keys [uid email rating]}]]
   ;; We can store a default rating or fetch from Firestore
   (fb/store-user! uid email)
   {:db (-> db
            (assoc :user {:uid uid
                          :email email
                          :rating (or rating 1200)})
            ;; Move to the schedule page
            (assoc :page :schedule))}))

;; Submitting a game result
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
                (update :log-of-rating-events conj rating-event))}))))
