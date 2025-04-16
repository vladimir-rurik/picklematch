(ns picklematch.events
  (:require
   [re-frame.core :as rf]
   [picklematch.firebase :as fb]
   [picklematch.rating :as rating]))

;; -- Initialize the app-db --
(rf/reg-event-db
 :initialize
 (fn [_ _]
   {:user nil
    :players {}  ;; map of uid->rating
    :games
    ;; For MVP, you can hardcode sample games or fetch them from Firestore.
    ;; Example:
    [{:id 1
      :time "8:00 AM"
      :team1 {:player1 "uid-a" :player2 "uid-b"}
      :team2 {:player1 "uid-c" :player2 "uid-d"}}
     {:id 2
      :time "9:00 AM"
      :team1 {:player1 "uid-e" :player2 "uid-f"}
      :team2 {:player1 "uid-g" :player2 "uid-h"}}]
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

;; Once sign-in is successful, you should dispatch :login-success with
;; user info that you get from the popup response.
;; For demonstration, we'll store user in the db with default rating:
(rf/reg-event-fx
 :login-success
 (fn [{:keys [db]} [_ {:keys [uid email]}]]
   (fb/store-user! uid email)
   {:db (assoc db :user {:uid uid
                         :email email})
    ;; Optionally, you can retrieve user's rating from Firestore and store it in :players
    }))

;; Example of storing game results (scores), then recalculating ratings:
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
       ;; Persist updated ratings in :players
       {:db (-> new-db
                (assoc :players updated-ratings)
                (update :log-of-rating-events conj rating-event))}))))
