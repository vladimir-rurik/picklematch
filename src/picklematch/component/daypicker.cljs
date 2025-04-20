(ns picklematch.component.daypicker
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   ["react-day-picker" :refer [DayPicker]]))

(defn date->yyyy-mm-dd [js-date]
  (let [y (.getFullYear js-date)
        m (inc (.getMonth js-date)) ; months 0-based
        d (.getDate js-date)]
    (str y "-" (when (< m 10) "0") m "-" (when (< d 10) "0") d)))


(defn daypicker-component []
  (let [all-game-dates @(rf/subscribe [:all-game-dates])  ;; e.g. #{"2025-05-01" "2025-05-03"}
        selected-date  @(rf/subscribe [:selected-date])]   ;; a js/Date or nil
    (r/create-element
     DayPicker
     #js {:showOutsideDays false
          :styles #js
                   {:day
                    (fn [dayObj _modifiers]
                      (let [js-date (aget dayObj "date")
                            day-str (date->yyyy-mm-dd js-date)
                            sel-str (date->yyyy-mm-dd selected-date) ;; selected date as string
                            ;; Check if the day has a game scheduled
                            has-game (contains? all-game-dates day-str)]
                        ;; Debug logs here:
                        (js/console.log "day-str:" day-str "sel-str:" sel-str "has-game:" has-game)

                        (cond
                          ;; If this day is the selected date, color it blue
                          (= day-str sel-str)
                          #js {:backgroundColor "lightblue"}

                          ;; Otherwise, if the day has a game, color it green
                          has-game
                          #js {:backgroundColor "lightgreen"}

                          :else
                          #js {})))}

          ;; When the user clicks a date, we set it selected and load its games
          :onDayClick
          (fn [dayObj _modifiers _evt]
            (let [js-date dayObj                                  ;; dayObj itself is the JS Date
                  day-str (date->yyyy-mm-dd js-date)]             ;; convert directly to string
              ;; Dispatch events to set the selected date and load games for that date
              (rf/dispatch [:set-selected-date js-date])
              (rf/dispatch [:load-games-for-date day-str])))}
     nil)))
