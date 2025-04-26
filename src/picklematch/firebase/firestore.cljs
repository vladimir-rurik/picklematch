(ns picklematch.firebase.firestore
  (:require
   [picklematch.firebase.user :as user]
   [picklematch.firebase.game :as game]))

;; Re-export user functions
(def store-user-if-new! user/store-user-if-new!)
(def load-user-doc! user/load-user-doc!)
(def update-user-role! user/update-user-role!)
(def update-user-rating! user/update-user-rating!)
(def load-all-users! user/load-all-users!)
(def set-user-active! user/set-user-active!)

;; Re-export game functions
(def add-game! game/add-game!)
(def load-games-for-date! game/load-games-for-date!)
(def register-for-game! game/register-for-game!)
(def store-game-score! game/store-game-score!)
(def load-all-game-dates! game/load-all-game-dates!)
(def add-game-with-players! game/add-game-with-players!)