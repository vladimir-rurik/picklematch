(ns picklematch.firebase
  (:require
   [picklematch.config :refer [firebase-config]]
   ["firebase/app" :refer [initializeApp getApps]] ;; <--- include getApps here
   ["firebase/auth" :refer [getAuth]]
   ["firebase/firestore" :as firestore]))

(defonce app
  (initializeApp (clj->js firebase-config)))

;; Now getApps is properly recognized
(js/console.log "Firebase apps so far:" (getApps))

(defonce auth-inst
  (getAuth app))

(defonce db
  (firestore/getFirestore app))