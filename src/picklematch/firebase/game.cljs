(ns picklematch.firebase.game
  (:require
   [picklematch.firebase.init :refer [db]]
   ["firebase/firestore" :as firestore]))

;; Game-specific Firestore operations
(defn add-game! [date-str time-str location-id on-success on-fail]
  (let [games-collection (firestore/collection db "games")
        doc-data {:date date-str
                  :time time-str
                  :location-id location-id
                  :team1 {:player1 nil :player2 nil}
                  :team2 {:player1 nil :player2 nil}
                  :team1-score1 0
                  :team1-score2 0
                  :team2-score1 0
                  :team2-score2 0}]
    (-> (firestore/addDoc games-collection (clj->js doc-data))
        (.then (fn [doc-ref] (on-success (.-id doc-ref))))
        (.catch on-fail))))

(defn load-games-for-date! [date-str on-success on-fail]
  (let [games-collection (firestore/collection db "games")
        q (-> (firestore/query games-collection
                               (firestore/where "date" "==" date-str)
                               (firestore/orderBy "time")))]
    (-> (firestore/getDocs q)
        (.then (fn [query-snapshot]
                 (let [games (for [doc (.-docs query-snapshot)]
                               (assoc (js->clj (.data doc) :keywordize-keys true)
                                      :id (.-id doc)))]
                   (on-success (vec games)))))
        (.catch on-fail))))

(defn register-for-game! [game-id team-key uid email on-success on-fail]
  (let [doc-ref (firestore/doc (firestore/collection db "games") game-id)]
    (-> (firestore/runTransaction db
                                  (fn [transaction]
                                    (-> (.get transaction doc-ref)
                                        (.then
                                         (fn [snapshot]
                                           (let [data (js->clj (.data snapshot) :keywordize-keys true)
                                                 t1   (get data (keyword team-key))
                                                 p1   (get t1 :player1)
                                                 p2   (get t1 :player2)]
                                             (cond
                                               (nil? p1)
                                               {team-key {:player1 uid
                                                          :player1-email email
                                                          :player2 (:player2 t1)}}

                                               (nil? p2)
                                               {team-key {:player1 (:player1 t1)
                                                          :player2 uid
                                                          :player2-email email}}

                                               :else
                                               {})))))))
        (.then on-success)
        (.catch on-fail))))

(defn store-game-score! [updated-game]
  (let [game-id  (:id updated-game)
        doc-ref  (firestore/doc (firestore/collection db "games") game-id)]
    (-> (firestore/updateDoc doc-ref
                             (clj->js {:team1-score1 (:team1-score1 updated-game)
                                       :team1-score2 (:team1-score2 updated-game)
                                       :team2-score1 (:team2-score1 updated-game)
                                       :team2-score2 (:team2-score2 updated-game)}))
        (.then #(js/console.log "Updated game scores for" game-id))
        (.catch #(js/console.error "Error updating game scores:" %)))))

(defn load-all-game-dates! [on-success on-fail]
  (let [col-ref (firestore/collection db "games")]
    (-> (firestore/getDocs col-ref)
        (.then (fn [snapshot]
                 (let [docs  (.-docs snapshot)
                       dates (map #(-> (js->clj (.data %)) (get "date"))
                                  docs)]
                   (on-success (into #{} dates)))))
        (.catch on-fail))))

(defn add-game-with-players! [doc-data on-success on-fail]
  (let [games-col (firestore/collection db "games")]
    (-> (firestore/addDoc games-col (clj->js doc-data))
        (.then (fn [doc-ref] (on-success (.-id doc-ref))))
        (.catch on-fail))))

(defn delete-game! [game-id on-success on-fail]
  (let [doc-ref (firestore/doc (firestore/collection db "games") game-id)]
    (-> (firestore/deleteDoc doc-ref)
        (.then on-success)
        (.catch on-fail))))
