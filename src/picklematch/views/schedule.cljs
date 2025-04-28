(ns picklematch.views.schedule
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [clojure.string :as str]
   [picklematch.util :refer [format-date-obj-to-iso-str]])) ; Require the util namespace

(defn schedule-game-panel []
  (let [date-str (r/atom "")
        time-str (r/atom "")
        selected-location-id @(rf/subscribe [:selected-location-id])] ; Use the selected location ID from app state
    (fn []
      [:div.fancy-panel
       [:h3 "Schedule a new game"]
       [:label "Date: "]
       [:input {:type "date"
                :on-change #(reset! date-str (.. % -target -value))}]
       [:br]
       [:label "Time: "]
       [:input {:type "text"
                :placeholder "e.g. 18:30"
                :on-change #(reset! time-str (.. % -target -value))}]
       [:br]
       [:button.btn-secondary
        {:on-click #(rf/dispatch [:schedule-game @date-str @time-str selected-location-id])}
        "Add Game"]])))

(defn admin-panel []
  (let [times (r/atom ["08:00" "09:00" "10:00"])
        selected-date (rf/subscribe [:selected-date])] ; Subscribe to selected date
    (fn []
      [:div.fancy-panel
       [schedule-game-panel]
       [:h4 "Auto-Assign Courts"]
       [:p "Times (comma-separated):"]
       [:input {:type "text"
                :value (str/join "," @times)
                :on-change #(reset! times (-> % .-target .-value (str/split #",")))}]
       [:button.btn-secondary
        {:on-click #(let [date-str (format-date-obj-to-iso-str @selected-date)]
                      (if date-str
                        (rf/dispatch [:auto-assign-players date-str @times])
                        (js/alert "Please select a date first!")))} ; Use formatted local date
        "Auto-Assign"]])))
