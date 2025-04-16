(ns picklematch.matching
  "Responsible for generating new games (matchups) based on player ratings."
  (:require [clojure.string :as str]))

(defn sort-players-by-rating
  "Sort players by descending rating.
   players-map is a {uid rating} map. Returns a vector of [uid rating] sorted."
  [players-map]
  (->> players-map
       (sort-by val >)
       (into [])))  ;; e.g. [[:uid-a 1250] [:uid-b 1200] ...]

(defn group-into-quads
  "Given a list of sorted [uid rating], group into sets of four players
   for each court. Returns a seq of groups: [ [p1 p2 p3 p4] [p5 p6 p7 p8] ... ]."
  [sorted-players]
  (partition 4 4 nil sorted-players))

(defn generate-games
  "Given a map of players {uid rating}, produce a sequence of game data
   suitable for storing in :games. Example of a game item:
   {:id 123
    :time \"10:00 AM\"
    :team1 {:player1 uid1 :player2 uid2}
    :team2 {:player1 uid3 :player2 uid4}}.

   For an MVP, we might not care about time, or we can dummy-in incremental times."
  [players-map]
  (let [sorted-players (sort-players-by-rating players-map)
        quads (group-into-quads sorted-players)]
    (map-indexed
     (fn [i quad]
       (let [[p1 p2 p3 p4] (map first quad)
             game-id       (inc i)
             time-str      (str (+ 8 i) ":00 AM")]  ;; naive approach for example
         {:id game-id
          :time time-str
          :team1 {:player1 p1 :player2 p2}
          :team2 {:player1 p3 :player2 p4}}))
     quads)))
