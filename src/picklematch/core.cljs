(ns picklematch.core
  (:require
   [re-frame.core :as rf]
   [picklematch.events]
   [picklematch.subs]
   [picklematch.views :refer [main-panel]]
   [reagent.dom :as rdom]
   [picklematch.firebase :as fb]))

(defn ^:dev/after-load mount-root []
  (rdom/render [main-panel]
               (.getElementById js/document "app")))

(defn init []
  (rf/dispatch-sync [:initialize])
  (mount-root))