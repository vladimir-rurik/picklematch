(ns picklematch.rating)

(defn recalculate-ratings
  "Given the db (with :players) and the updated-game,
   returns [new-player-ratings rating-event-log]."
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
                 :else nil) ;; handle tie
        loser (if (= winner team1-players) team2-players team1-players)

        players (:players db)  ;; Suppose db has a map: {player-name rating}
        rating-update (fn [p delta]
                        (update players p (fnil + 1200) delta))]

    (if (and winner loser)
      (let [new-ratings (reduce #(rating-update %1 %2) players
                                (concat (map (fn [p] [p 5]) winner)
                                        (map (fn [p] [p -5]) loser)))]
        [new-ratings
         {:game-id (:id updated-game)
          :winners winner
          :losers loser}])
      [players {}])))