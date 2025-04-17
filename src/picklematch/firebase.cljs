(ns picklematch.firebase
  (:require
   [picklematch.config :refer [firebase-config]]
   ["firebase/app" :refer [initializeApp getApps]]
   ["firebase/auth" :refer [getAuth
                            GoogleAuthProvider
                            FacebookAuthProvider
                            signInWithPopup
                            signOut]]
   ["firebase/firestore" :as firestore]))


;; ---------------------------------------------------------
;; Initialize Firebase
;; ---------------------------------------------------------
(defonce app
  (if (empty? (getApps))
    (initializeApp (clj->js firebase-config))
    (first (getApps))))

(js/console.log "Firebase apps so far:" (getApps))

(defonce auth-inst
  (getAuth app))

(defonce db
  (firestore/getFirestore app))

;; ---------------------------------------------------------
;; Auth Helpers
;; ---------------------------------------------------------
(defn google-sign-in []
  (let [provider (GoogleAuthProvider.)]
    (-> (signInWithPopup auth-inst provider)
        (.then (fn [result]
                 (js/console.log "Google sign-in success" result)))
        (.catch (fn [err]
                  (js/console.error "Google sign-in error" err))))))

(defn facebook-sign-in []
  (let [provider (FacebookAuthProvider.)]
    (-> (signInWithPopup auth-inst provider)
        (.then (fn [result]
                 (js/console.log "Facebook sign-in success" result)))
        (.catch (fn [err]
                  (js/console.error "Facebook sign-in error" err))))))

;; ---------------------------------------------------------
;; Firestore Helpers
;; ---------------------------------------------------------
(defn store-user-if-new!
  "Create user doc if not already present, with default rating of 1200 and role=ordinary."
  [uid email]
  (let [doc-ref (firestore/doc (firestore/collection db "users") uid)]
    (-> (firestore/getDoc doc-ref)
       (.then
        (fn [snapshot]
          ;; Check existence of doc
          (if-not (.exists snapshot)
            ;; If doesn't exist, create it
            (-> (firestore/setDoc doc-ref
                                  (clj->js {:uid uid
                                            :email email
                                            :rating 1200
                                            :role "ordinary"}))
                (.then #(js/console.log "New user doc created for" uid))
                (.catch #(js/console.error "Error creating user doc:" %)))
            ;; Else: do nothing
            (js/console.log "User doc already exists for" uid))))
       (.catch #(js/console.error "Error checking user in Firestore:" %)))))


(defn load-user-doc!
  [uid on-success on-fail]
  (let [doc-ref (firestore/doc (firestore/collection db "users") uid)]
    (-> (firestore/getDoc doc-ref)
        (.then (fn [snapshot]
                 (if (.-exists snapshot)
                   (on-success (js->clj (.data snapshot) :keywordize-keys true))
                   (on-success nil))))
        (.catch on-fail))))

(defn update-user-role!
  [uid new-role]
  (let [doc-ref (firestore/doc (firestore/collection db "users") uid)]
    (-> (firestore/updateDoc doc-ref (clj->js {:role new-role}))
        (.then #(js/console.log (str "Updated user " uid " role to " new-role)))
        (.catch #(js/console.error "Error updating role:" %)))))

(defn update-user-rating!
  [uid new-rating]
  (let [doc-ref (firestore/doc (firestore/collection db "users") uid)]
    (-> (firestore/updateDoc doc-ref (clj->js {:rating new-rating}))
        (.then #(js/console.log "Updated user rating:" uid new-rating))
        (.catch #(js/console.error "Error updating user rating:" %)))))

;; ---------------------------------------------------------
;; Games
;; We'll store each game in a "games" collection, with a date field,
;; auto-generated ID, etc.
;; ---------------------------------------------------------
(defn add-game!
  [date-str time-str on-success on-fail]
  (let [games-collection (firestore/collection db "games")
        doc-data {:date date-str
                  :time time-str
                  :team1 {:player1 nil :player2 nil}
                  :team2 {:player1 nil :player2 nil}
                  :team1-score 0
                  :team2-score 0}]
    (-> (firestore/addDoc games-collection (clj->js doc-data))
        (.then (fn [doc-ref]
                 (on-success (.-id doc-ref))))
        (.catch on-fail))))

(defn load-games-for-date!
  [date-str on-success on-fail]
  (let [games-collection (firestore/collection db "games")
        q (-> (firestore/query games-collection
                               (firestore/where "date" "==" date-str)
                               (firestore/orderBy "time")))]
    (js/console.log "Loading games for date:" date-str)
    (-> (firestore/getDocs q)
        (.then (fn [query-snapshot]
                 (let [games (for [doc (.-docs query-snapshot)]
                               (assoc (js->clj (.data doc) :keywordize-keys true)
                                      :id (.-id doc)))]
                   (on-success (vec games)))))
        (.catch on-fail))))

(defn register-for-game!
  [game-id team-key uid email on-success on-fail]
  (let [doc-ref (firestore/doc (firestore/collection db "games") game-id)
        team-path (str team-key ".player1") ;; for simplicity, just fill player1 if nil, else player2
        update-fn (fn [snapshot]
                    (let [data (js->clj (.data snapshot) :keywordize-keys true)
                          existing1 (get-in data [(keyword team-key) :player1])
                          existing2 (get-in data [(keyword team-key) :player2])]
                      (cond
                        (nil? existing1)
                        {(keyword team-key) {:player1 uid :player1-email email
                                             :player2 existing2}}

                        (nil? existing2)
                        {(keyword team-key) {:player1 existing1
                                             :player2 uid
                                             :player2-email email}}

                        :else
                        {})))]
    (-> (firestore/runTransaction db
                                  (fn [transaction]
                                    (-> (.get transaction doc-ref)
                                        (.then (fn [snapshot]
                                                 (let [new-data (update-fn snapshot)]
                                                   (when (seq new-data)
                                                     (.update transaction doc-ref (clj->js new-data)))))))))
        (.then on-success)
        (.catch on-fail))))

(defn store-game-score!
  [updated-game]
  (let [game-id (:id updated-game)
        doc-ref (firestore/doc (firestore/collection db "games") game-id)]
    (-> (firestore/updateDoc doc-ref
                             (clj->js {:team1-score (:team1-score updated-game)
                                       :team2-score (:team2-score updated-game)}))
        (.then #(js/console.log "Updated game scores for" game-id))
        (.catch #(js/console.error "Error updating game scores:" %))))) 

(defn logout []
  (-> (signOut auth-inst)
      (.then (fn []
               (js/console.log "Successfully signed out.")))
      (.catch (fn [err]
                (js/console.error "Error signing out:" err)))))