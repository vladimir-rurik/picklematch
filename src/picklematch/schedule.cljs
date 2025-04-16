(ns picklematch.schedule)

(defn generate-schedule
  "Given a list of players, a sequence of courts, and a set of times,
   produce a lazy schedule. This is a placeholder logic you can refine
   to match your real scheduling approach."
  [players court-sequence time-slots]
  (let [player-count (count players)
        ;; Each court needs exactly 4 players for a game
        max-courts (min (count court-sequence) (int (/ player-count 4)))
        used-courts (take max-courts court-sequence)
        total-slots (count time-slots)]
    (loop [t-idx 0
           p-idx 0
           schedule []]
      (if (or (>= t-idx total-slots)
              (<= (count (drop p-idx players)) 3))
        ;; Return the schedule plus leftover waiting players
        (let [waiting (drop p-idx players)]
          {:games schedule
           :waiting waiting})
        (let [this-time (nth time-slots t-idx)
              round-games
              (map-indexed
               (fn [i court]
                 ;; for each court, pick the next 4 players
                 (let [game-players (subvec (vec players)
                                            (+ p-idx (* i 4))
                                            (+ p-idx (* i 4) 4))]
                   {:time    this-time
                    :court   court
                    :players game-players}))
               used-courts)
              new-schedule (concat schedule round-games)
              ;; after assigning 4 players per used-court
              next-p-idx (+ p-idx (* 4 (count used-courts)))]
          (recur (inc t-idx)
                 next-p-idx
                 new-schedule))))))
