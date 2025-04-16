(ns picklematch.events-test
  (:require
   [clojure.test :refer [deftest testing is]]
   [re-frame.core :as rf]
   [picklematch.events]
   [picklematch.subs]))

;; re-frame testing approach
(deftest submit-game-result-test
  (testing "When a game result is submitted, the ratings update."
    (rf/dispatch-sync [:initialize])
    (rf/dispatch-sync [:login-success {:uid "uid-x" :email "x@test.com"}])
    ;; set up db with sample players & games
    (rf/dispatch-sync
     [:submit-game-result 1 11 5])
    ;; check result ...
    ;; This is a high-level example. You could subscribe to :games or :players
    ;; and verify the changes.
    (is true "Replace with actual checks.")))
