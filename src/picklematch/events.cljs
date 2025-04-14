(ns picklematch.events
  (:require
   [re-frame.core :as rf]
   [picklematch.firebase :as fb]
   [picklematch.rating :as rating]))

(rf/reg-event-db
 :initialize
 (fn [_ _]
   {:user nil
    :games []
    :current-session-date (js/Date.)}))

(rf/reg-event-fx
 :sign-in-with-google
 (fn [{:keys [db]} _]
   {:db db
    :firebase/google-sign-in true}))

(rf/reg-fx
 :firebase/google-sign-in
 (fn [_]
   (fb/google-sign-in)))

;; Suppose when login success fires an event from somewhere:
(rf/reg-event-fx
 :login-success
 (fn [{:keys [db]} [_ user-info]]
   (let [{:keys [uid email]} user-info]
     (fb/store-user! uid email)
     {:db (assoc db :user user-info)})))

;; Example of storing game results (scores), then recalculating ratings:
(rf/reg-event-fx
 :submit-game-result
 (fn [{:keys [db]} [_ game-id team1-score team2-score]]
   (let [game (some #(when (= (:id %) game-id) %) (:games db))
         updated-game (assoc game
                             :team1-score team1-score
                             :team2-score team2-score)
         new-db (update db :games
                        (fn [gs] (map (fn [g] (if (= (:id g) game-id)
                                                updated-game
                                                g))
                                      gs)))]

     ;; Recalculate ratings in a simple manner
     (let [[updated-ratings rating-events]
           (rating/recalculate-ratings new-db updated-game)]

       ;; You'd also persist these rating updates to Firestore
       ;; e.g. rating/store-ratings! updated-ratings

       {:db (assoc new-db
                   :players updated-ratings
                   :log-of-rating-events (conj (:log-of-rating-events db)
                                               rating-events))}))))