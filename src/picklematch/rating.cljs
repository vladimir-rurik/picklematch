(ns picklematch.rating
  (:require
   [picklematch.firebase :as fb]))

(defn recalculate-ratings
  "Given the db (with :players) and updated-game,
   returns [new-player-ratings rating-event-log].
   Basic placeholder logic: +5 to winners, -5 to losers."
  [db updated-game]
  (let [team1-score (:team1-score updated-game)
        team2-score (:team2-score updated-game)
        team1 (get updated-game :team1 {})
        team2 (get updated-game :team2 {})
        team1-players (filter some? [(:player1 team1) (:player2 team1)])
        team2-players (filter some? [(:player1 team2) (:player2 team2)])
        winner (cond
                 (> team1-score team2-score) team1-players
                 (> team2-score team1-score) team2-players
                 :else nil)
        loser (cond
                (= winner team1-players) team2-players
                (= winner team2-players) team1-players
                :else nil)
        players (:players db) ;; {uid {:rating .. :role ..}}
        rating-update (fn [acc uid delta]
                        (update-in acc [uid :rating]
                                   (fnil #(+ % delta) 1200)))]
    (if (and (seq winner) (seq loser))
      (let [new-acc (reduce (fn [acc uid]
                              (rating-update acc uid 5))
                            players
                            winner)
            new-acc (reduce (fn [acc uid]
                              (rating-update acc uid -5))
                            new-acc
                            loser)]
        ;; Persist rating changes in Firestore
        (doseq [[uid info] new-acc]
          (fb/update-user-rating! uid (:rating info)))
        [new-acc
         {:game-id (:id updated-game)
          :winners winner
          :losers  loser}])
      [players {}])))
