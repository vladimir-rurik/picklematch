(ns picklematch.test-runner
  (:require
   [picklematch.events-test]
   [picklematch.matching-test]
   [picklematch.rating-test]
   [cljs.test :refer [run-tests]]))

(defn ^:export run []
  (run-tests
   'picklematch.events-test
   'picklematch.matching-test
   'picklematch.rating-test))
