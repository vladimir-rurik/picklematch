(ns picklematch.firebase
  (:require
   ["firebase/app" :as firebase]
   ["firebase/auth" :as auth]
   ["firebase/firestore" :as firestore]))

;; Replace these with your actual config from Firebase console
(defonce firebase-config
  {:apiKey "YOUR-API-KEY"
   :authDomain "yourapp.firebaseapp.com"
   :projectId "yourapp"
   :storageBucket "yourapp.appspot.com"
   :messagingSenderId "SENDER-ID"
   :appId "APP-ID"})

(defonce app
  (if (not (.-apps firebase))
    (firebase/initializeApp (clj->js firebase-config))
    (.-app firebase)))

(defonce db (firestore/getFirestore app))
(defonce auth-instance (auth/getAuth app))

(defn google-sign-in []
  (let [provider (auth/GoogleAuthProvider.)]
    (-> (auth/signInWithPopup auth-instance provider)
        (.then (fn [result]
                 (js/console.log "Google user:" (.-user result))))
        (.catch (fn [error]
                  (js/console.error error))))))

(defn facebook-sign-in []
  (let [provider (auth/FacebookAuthProvider.)]
    (-> (auth/signInWithPopup auth-instance provider)
        (.then (fn [result]
                 (js/console.log "Facebook user:" (.-user result))))
        (.catch (fn [error]
                  (js/console.error error))))))

(defn store-user! [uid email]
  (-> (firestore/setDoc
       (firestore/doc db "users" uid)
       (clj->js {:email email
                 :rating 1200  ;; default or pull from existing
                 }))
      (.then #(js/console.log "Stored user in Firestore"))))