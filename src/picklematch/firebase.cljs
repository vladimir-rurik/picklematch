(ns picklematch.firebase
  (:require
   [picklematch.config :refer [firebase-config]]
   ["firebase/app" :refer [initializeApp getApps]]
   ["firebase/auth" :refer [getAuth GoogleAuthProvider FacebookAuthProvider signInWithPopup]]
   ["firebase/firestore" :as firestore]))

;; Initialize the Firebase app only if not already initialized
(defonce app
  (if (empty? (getApps))
    (initializeApp (clj->js firebase-config))
    (first (getApps))))

(js/console.log "Firebase apps so far:" (getApps))

(defonce auth-inst
  (getAuth app))

(defonce db
  (firestore/getFirestore app))

;; Google sign-in
(defn google-sign-in []
  (let [provider (GoogleAuthProvider.)]
    (-> (signInWithPopup auth-inst provider)
        (.then (fn [result]
                 (js/console.log "Google sign-in success" result)))
        (.catch (fn [err]
                  (js/console.error "Google sign-in error" err))))))

;; Facebook sign-in
(defn facebook-sign-in []
  (let [provider (FacebookAuthProvider.)]
    (-> (signInWithPopup auth-inst provider)
        (.then (fn [result]
                 (js/console.log "Facebook sign-in success" result)))
        (.catch (fn [err]
                  (js/console.error "Facebook sign-in error" err))))))

;; Store a new or existing user in Firestore
(defn store-user!
  [uid email]
  (let [users-col (firestore/collection db "users")]
    (-> (firestore/setDoc
         (firestore/doc users-col uid)
         (clj->js {:uid uid
                   :email email
                   :rating 1200}))
        (.then #(js/console.log "Stored user in Firestore:" uid))
        (.catch #(js/console.error "Error storing user in Firestore:" %)))))

;; Update user rating in Firestore
(defn update-user-rating!
  [uid new-rating]
  (let [doc-ref (firestore/doc (firestore/collection db "users") uid)]
    (-> (firestore/updateDoc doc-ref (clj->js {:rating new-rating}))
        (.then #(js/console.log "Updated user rating:" uid new-rating))
        (.catch #(js/console.error "Error updating user rating:" %)))))
