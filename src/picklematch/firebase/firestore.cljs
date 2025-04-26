(ns picklematch.firebase.firestore
  (:require
   [picklematch.firebase.init :refer [db auth-inst]]
   ["firebase/firestore" :as firestore]))

;; ------------------------------------------------------------------
;; Firestore: User Docs
;; ------------------------------------------------------------------
(defn store-user-if-new!
  "Create user doc if not already present, with optional :active setting.
   Defaults to active? = true for e.g. Google sign-in. 
   For email/password sign-up, we can pass false."
  ([uid email]
   (store-user-if-new! uid email true))  ; by default, active=true
  ([uid email active?]
   (let [doc-ref (firestore/doc (firestore/collection db "users") uid)]
     (-> (firestore/getDoc doc-ref)
         (.then
          (fn [snapshot]
            (if-not (.exists snapshot)
              (-> (firestore/setDoc doc-ref
                                    (clj->js {:uid uid
                                              :email email
                                              :rating 1000
                                              :role "ordinary"
                                              :active active?}))
                  (.then #(js/console.log "New user doc created for" uid))
                  (.catch #(js/console.error "Error creating user doc:" %)))
              (js/console.log "User doc already exists for" uid))))
         (.catch #(js/console.error "Error checking user in Firestore:" %))))))

(defn load-user-doc! [uid on-success on-fail]
  (let [doc-ref (firestore/doc (firestore/collection db "users") uid)]
    (-> (firestore/getDoc doc-ref)
        (.then (fn [snapshot]
                 (if (.-exists snapshot)
                   (on-success (js->clj (.data snapshot) :keywordize-keys true))
                   (on-success nil))))
        (.catch on-fail))))

(defn update-user-role! [uid new-role]
  (let [doc-ref (firestore/doc (firestore/collection db "users") uid)]
    (-> (firestore/updateDoc doc-ref #js {:role new-role})
        (.then #(js/console.log (str "Updated user " uid " role to " new-role)))
        (.catch #(js/console.error "Error updating role:" %)))))

(defn update-user-rating! [uid new-rating]
  (let [doc-ref (firestore/doc (firestore/collection db "users") uid)]
    (-> (firestore/updateDoc doc-ref #js {:rating new-rating})
        (.then #(js/console.log "Updated user rating:" uid new-rating))
        (.catch #(js/console.error "Error updating user rating:" %)))))

(defn load-all-users! [on-success on-fail]
  (let [col-ref (firestore/collection db "users")]
    (-> (firestore/getDocs col-ref)
        (.then (fn [snapshot]
                 (let [docs  (.-docs snapshot)
                       users (into {}
                                   (map (fn [doc]
                                          (let [udata (js->clj (.data doc) :keywordize-keys true)
                                                uid   (:uid udata)]
                                            [uid udata])))
                                   docs)]
                   (on-success users))))
        (.catch on-fail))))

(defn set-user-active! [uid active?]
  (let [doc-ref (firestore/doc (firestore/collection db "users") uid)]
    (-> (firestore/setDoc doc-ref #js {:active active?} #js {:merge true})
        (.then #(js/console.log "Set user doc: active=" active?))
        (.catch #(js/console.error "Error setting user doc active:" %)))))

;; ------------------------------------------------------------------
;; Firestore: Games
;; ------------------------------------------------------------------
(defn add-game! [date-str time-str on-success on-fail]
  (let [games-collection (firestore/collection db "games")
        doc-data {:date date-str
                  :time time-str
                  :team1 {:player1 nil :player2 nil}
                  :team2 {:player1 nil :player2 nil}
                  :team1-score 0
                  :team2-score 0}]
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
                             (clj->js {:team1-score (:team1-score updated-game)
                                       :team2-score (:team2-score updated-game)}))
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