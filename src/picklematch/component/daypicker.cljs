(ns picklematch.component.daypicker
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   ["react-day-picker" :refer [DayPicker]]))

(defn date->yyyy-mm-dd [js-date]
  (let [y (.getFullYear js-date)
        m (inc (.getMonth js-date))
        d (.getDate js-date)]
    (str y "-" (when (< m 10) "0") m "-" (when (< d 10) "0") d)))

(defn daypicker-component []
  (let [all-game-dates @(rf/subscribe [:all-game-dates])
        selected-date  @(rf/subscribe [:selected-date])
        sel-str (when selected-date (date->yyyy-mm-dd selected-date))]
    (r/create-element
     DayPicker
     #js {:showOutsideDays false
          :modifiers #js {:hasGame
                          (fn [date]
                            (contains? all-game-dates (date->yyyy-mm-dd date)))
                          :selected
                          (fn [date]
                            (= (date->yyyy-mm-dd date) sel-str))
                          :selectedHasGame
                          (fn [date]
                            (let [day-str (date->yyyy-mm-dd date)]
                              (and (= day-str sel-str)
                                   (contains? all-game-dates day-str))))}
          :modifiersStyles
          #js {:selectedHasGame #js {:backgroundColor "orange"}
               :selected #js {:backgroundColor "lightblue"}
               :hasGame #js {:backgroundColor "lightgreen"}}
          :onDayClick
          (fn [dayObj _modifiers _evt]
            (let [day-str (date->yyyy-mm-dd dayObj)]
              (rf/dispatch [:set-selected-date dayObj])
              (rf/dispatch [:load-games-for-date day-str])))}
     nil)))
