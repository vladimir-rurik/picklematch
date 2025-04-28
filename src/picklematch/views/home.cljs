(ns picklematch.views.home
  (:require
   [re-frame.core :as rf]
   [picklematch.component.daypicker :refer [daypicker-component]]
   [picklematch.views.schedule :refer [admin-panel]]
   [picklematch.views.games :refer [game-list]]
   [clojure.string :as str]
   [reagent.core :as r]))

(defn home-panel []
  (let [user   @(rf/subscribe [:user])
        role   (:role user)
        email  (:email user)
        rating (get-in @(rf/subscribe [:players]) [(-> user :uid) :rating])
        locations @(rf/subscribe [:locations])
        selected-location @(rf/subscribe [:selected-location])
        selected-location-id @(rf/subscribe [:selected-location-id])] ; Get selected location ID
    [:div
     [:div.header-bar
      [:h2 (str "Welcome, " (or (some-> email (str/split #"@") first) "anonymous")
                (when rating (str " (" rating ")")))]
      [:button.btn-secondary
       {:on-click #(rf/dispatch [:logout])}
       "Logout"]]
     [:div.location-selector
      [:label "Location:"]
      [:select.fancy-select
       {:value (or selected-location-id "")
        :style {:width "200px"}
        :on-change #(rf/dispatch [:set-selected-location (.. % -target -value)])} ; Dispatch event on change
       (for [loc locations]
         ^{:key (:id loc)} [:option {:value (:id loc)} (:name loc)])]]
     [daypicker-component]
     (when (= role "admin")
       [admin-panel])
     [game-list]]))
