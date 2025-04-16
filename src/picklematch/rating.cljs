(ns picklematch.rating
  (:require
   [picklematch.firebase :as fb]))

(defn recalculate-ratings
  "Given the db (with :players) and the updated game,
   returns [new-player-ratings rating-event].
   Very basic logic: winner +5, loser -5."
  [db updated-game]
  (let [team1-score (:team1-score updated-game)
        team2-score (:team2-score updated-game)
        team1-players [(get-in updated-game [:team1 :player1])
                       (get-in updated-game [:team1 :player2])]
        team2-players [(get-in updated-game [:team2 :player1])
                       (get-in updated-game [:team2 :player2])]
        winner (cond
                 (> team1-score team2-score) team1-players
                 (> team2-score team1-score) team2-players
                 :else nil) ;; handle tie
        loser (if (= winner team1-players) team2-players team1-players)
        players (:players db)  ;; Suppose players is {uid rating-int}
        rating-update (fn [acc uid delta]
                        (update acc uid (fnil + 1200) delta))]

    (if (and winner loser)
      (let [new-map (reduce (fn [acc uid] (rating-update acc uid 5))
                            players
                            winner)
            new-map (reduce (fn [acc uid] (rating-update acc uid -5))
                            new-map
                            loser)]
        ;; Persist changes to Firestore
        (doseq [[uid new-rating] new-map]
          (fb/update-user-rating! uid new-rating))
        [new-map
         {:game-id (:id updated-game)
          :winners winner
          :losers  loser}])
      [players {}])))
