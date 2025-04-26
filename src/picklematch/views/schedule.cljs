(ns picklematch.views.schedule
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [clojure.string :as str]))

(defn schedule-game-panel []
  (let [date-str (r/atom "")
        time-str (r/atom "")]
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
       [:button.btn-secondary
        {:on-click #(rf/dispatch [:schedule-game @date-str @time-str])}
        "Add Game"]])))

(defn admin-panel []
  (let [times (r/atom ["08:00" "09:00" "10:00"])]
    (fn []
      [:div.fancy-panel
       [schedule-game-panel]
       [:h4 "Auto-Assign Courts"]
       [:p "Times (comma-separated):"]
       [:input {:type "text"
                :value (str/join "," @times)
                :on-change #(reset! times (-> % .-target .-value (str/split #",")))}]
       [:button.btn-secondary
        {:on-click #(rf/dispatch [:auto-assign-players
                                  (-> (js/Date.) .toISOString (subs 0 10))
                                  @times])}
        "Auto-Assign"]])))