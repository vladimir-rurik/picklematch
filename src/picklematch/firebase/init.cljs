(ns picklematch.firebase.init
  (:require
   [picklematch.config :as cfg]
   ["firebase/app" :refer [initializeApp getApps]]
   ["firebase/auth" :refer [getAuth]]
   ["firebase/firestore" :as firestore]))

;; Initialize the Firebase App (only once)
(defonce app
  (if (empty? (getApps))
    (initializeApp
     #js {:apiKey            cfg/FIREBASE_API_KEY
          :authDomain        cfg/FIREBASE_AUTH_DOMAIN
          :projectId         cfg/FIREBASE_PROJECT_ID
          :storageBucket     cfg/FIREBASE_STORAGE_BUCKET
          :messagingSenderId cfg/FIREBASE_MESSAGING_SENDER_ID
          :appId             cfg/FIREBASE_APP_ID})
    (first (getApps))))

;; Expose auth instance and Firestore db
(defonce auth-inst (getAuth app))
(defonce db (firestore/getFirestore app))