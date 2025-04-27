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
        location (r/atom "Tondiraba Indoor")] ; Default location
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
       {:value @location
        :style {:width "200px"}
        :on-change #(reset! location (.. % -target -value))}
       [:option "Tondiraba Indoor"]
       [:option "Tondiraba Outdoor"]
       [:option "Koorti"]
       [:option "Golden Club"]
       [:option "Pirita"]]]
     [daypicker-component]
     (when (= role "admin")
       [admin-panel])
     [game-list]]))
