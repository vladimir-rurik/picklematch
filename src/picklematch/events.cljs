(ns picklematch.events
  (:require
   [re-frame.core :as rf]
   [picklematch.firebase :as fb]
   [picklematch.rating :as rating]
   [picklematch.matching :as matching]))

;; -- Initialize the app-db --
(rf/reg-event-db
 :initialize
 (fn [_ _]
   {:user nil
    :players {}  ;; map of uid->rating
    :games      [] ;; Start empty or fetch from Firestore for MVP
    :current-session-date (js/Date.)
    :log-of-rating-events []}))

;; -- Google sign-in flow --
(rf/reg-event-fx
 :sign-in-with-google
 (fn [{:keys [db]} _]
   {:db db
    :firebase/google-sign-in true}))

(rf/reg-fx
 :firebase/google-sign-in
 (fn [_]
   (fb/google-sign-in)))

;; -- Facebook sign-in flow --
(rf/reg-event-fx
 :sign-in-with-facebook
 (fn [{:keys [db]} _]
   {:db db
    :firebase/facebook-sign-in true}))

(rf/reg-fx
 :firebase/facebook-sign-in
 (fn [_]
   (fb/facebook-sign-in)))

;; -- Login success --
(rf/reg-event-fx
 :login-success
 (fn [{:keys [db]} [_ {:keys [uid email]}]]
   (fb/store-user! uid email)
   {:db (assoc db :user {:uid uid
                         :email email})}))

;; Submit game results (scores), recalc ratings
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

;; Generate next session’s games based on current players
(rf/reg-event-fx
 :generate-next-session
 (fn [{:keys [db]} _]
   (let [players (:players db)
         new-games (matching/generate-games players)]
     {:db (assoc db :games new-games)})))
