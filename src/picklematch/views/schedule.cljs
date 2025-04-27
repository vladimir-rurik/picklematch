(ns picklematch.views.schedule
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [clojure.string :as str]))

;; Helper to format Date object to YYYY-MM-DD based on local time
;; TODO: Move this to a shared utility namespace?
(defn format-date-obj-to-iso-str [date-obj]
  (when date-obj
    (let [year (.getFullYear date-obj)
          month-raw (inc (.getMonth date-obj)) ; Month is 0-indexed
          day-raw (.getDate date-obj)
          month (if (< month-raw 10) (str "0" month-raw) (str month-raw))
          day (if (< day-raw 10) (str "0" day-raw) (str day-raw))]
      (str year "-" month "-" day))))

(defn schedule-game-panel []
  (let [date-str (r/atom "")
        time-str (r/atom "")
        location (r/atom "Tondiraba Indoor")] ; Default location
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
       [:label "Location: "]
       [:select {:value @location
                 :on-change #(reset! location (.. % -target -value))}
        [:option "Tondiraba Indoor"]
        [:option "Tondiraba Outdoor"]
        [:option "Koorti"]
        [:option "Golden Club"]
        [:option "Pirita"]]
       [:br]
       [:button.btn-secondary
        {:on-click #(rf/dispatch [:schedule-game @date-str @time-str @location])}
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
