(ns picklematch.rating
  (:require
   [picklematch.firebase.firestore :as fbf]))

(defn recalculate-ratings
  "Given db (:players) and updated-game, returns [new-player-map rating-event].
   Winners get +5, losers get -5. Then updates Firestore."
  [db updated-game]
  (let [team1-score1 (js/parseInt (or (:team1-score1 updated-game) 0))
        team1-score2 (js/parseInt (or (:team1-score2 updated-game) 0))
        team2-score1 (js/parseInt (or (:team2-score1 updated-game) 0))
        team2-score2 (js/parseInt (or (:team2-score2 updated-game) 0))
        team1-total (+ team1-score1 team1-score2)
        team2-total (+ team2-score1 team2-score2)
        team1 (get updated-game :team1 {})
        team2 (get updated-game :team2 {})
        team1-players (filter some? [(:player1 team1) (:player2 team1)])
        team2-players (filter some? [(:player1 team2) (:player2 team2)])
        winner (cond
                 (> team1-total team2-total) team1-players
                 (> team2-total team1-total) team2-players
                 :else nil)
        loser (cond
                (= winner team1-players) team2-players
                (= winner team2-players) team1-players
                :else nil)
        players (:players db)
        rating-update (fn [acc uid delta]
                        (update-in acc [uid :rating]
                                   (fnil #(+ % delta) 1000)))]
    (if (and (seq winner) (seq loser))
      (let [new-acc (reduce (fn [acc uid] (rating-update acc uid 5)) players winner)
            new-acc (reduce (fn [acc uid] (rating-update acc uid -5)) new-acc loser)]
        ;; persist changes
        (doseq [[uid info] new-acc]
          (fbf/update-user-rating! uid (:rating info)))
        [new-acc
         {:game-id (:id updated-game)
          :winners winner
          :losers  loser}])
      [players {}])))
