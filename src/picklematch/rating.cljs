(ns picklematch.rating
  (:require
   [picklematch.firebase :as fb]))

(defn recalculate-ratings
  "Given the db (with :players) and the updated-game,
   returns [new-player-ratings rating-event-log].
   Basic placeholder logic: winning team +5, losing team -5."
  [db updated-game]
  (let [team1-score (:team1-score updated-game)
        team2-score (:team2-score updated-game)
        team1-players [(:player1 (:team1 updated-game))
                       (:player2 (:team1 updated-game))]
        team2-players [(:player1 (:team2 updated-game))
                       (:player2 (:team2 updated-game))]
        winner (cond
                 (> team1-score team2-score) team1-players
                 (> team2-score team1-score) team2-players
                 :else nil) ;; handle tie or no input
        loser (if (= winner team1-players) team2-players team1-players)
        players (:players db)  ;; Suppose db has a map like {uid rating}
        rating-update (fn [acc uid delta]
                        (update acc uid (fnil + 1200) delta))]
    (if (and winner loser)
      (let [new-acc (reduce (fn [acc uid]
                              (rating-update acc uid 5))
                            players
                            winner)
            new-acc (reduce (fn [acc uid]
                              (rating-update acc uid -5))
                            new-acc
                            loser)]
        ;; For each player, persist rating changes to Firestore
        (doseq [[uid new-rating] new-acc]
          (fb/update-user-rating! uid new-rating))
        [new-acc
         {:game-id (:id updated-game)
          :winners winner
          :losers  loser}])
      [players {}])))
