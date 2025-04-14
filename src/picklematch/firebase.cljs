(ns picklematch.firebase
  (:require
   [picklematch.config :refer [firebase-config]]
    ;; Firebase v9 modular imports
   ["firebase/app" :refer [initializeApp]]
   ["firebase/auth" :refer [getAuth]]
   ["firebase/firestore" :as firestore]))

;; Initialize the Firebase app (v9 modular style)
(defonce app
  (initializeApp (clj->js firebase-config)))

;; The Auth instance
(defonce auth-inst
  (getAuth app))

;; The Firestore instance
(defonce db
  (firestore/getFirestore app))