 (ns picklematch.component.daypicker
   (:require
    [re-frame.core :as rf]
    [reagent.core :as r]
    [picklematch.util :refer [format-date-obj-to-iso-str]] ; Use util function
    ["react-day-picker" :refer [DayPicker]]))

 (defn daypicker-component []
   (let [game-dates-for-location (or @(rf/subscribe [:game-dates-for-location]) #{}) ; Use new sub, default to empty set
         selected-date  @(rf/subscribe [:selected-date])
         sel-str (when selected-date (format-date-obj-to-iso-str selected-date))] ; Use imported fn
     (r/create-element
      DayPicker
      #js {:showOutsideDays false
           :modifiers #js {:hasGame ; Check against location-specific dates
                          (fn [date]
                            (contains? game-dates-for-location (format-date-obj-to-iso-str date)))
                          :selected
                          (fn [date]
                            (= (format-date-obj-to-iso-str date) sel-str))
                          :selectedHasGame ; Check against location-specific dates
                          (fn [date]
                            (let [day-str (format-date-obj-to-iso-str date)]
                              (and (= day-str sel-str)
                                   (contains? game-dates-for-location day-str))))}
           :modifiersStyles ; Keep existing styles for now
           #js {:selectedHasGame #js {:backgroundColor "orange"}
                :selected #js {:backgroundColor "lightblue"}
                :hasGame #js {:backgroundColor "lightgreen"}}
           :onDayClick
           (fn [dayObj _modifiers _evt]
             (let [day-str (format-date-obj-to-iso-str dayObj)] ; Use imported fn
               (rf/dispatch [:set-selected-date dayObj])
               (rf/dispatch [:load-games-for-date day-str])))} ; Keep this dispatch for loading games for the day view
      nil)))
