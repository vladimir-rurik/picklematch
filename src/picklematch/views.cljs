(ns picklematch.views
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]))

(defn login-panel []
  [:div.login
   [:h2 "PickleMatch Login"]
   [:button.btn-primary
    {:on-click #(rf/dispatch [:sign-in-with-google])}
    "Sign in with Google"]
   [:button.btn-primary
    {:on-click #(rf/dispatch [:sign-in-with-facebook])}
    "Sign in with Facebook"]])

(defn game-row [game]
  (let [team1-score (r/atom (or (:team1-score game) 0))
        team2-score (r/atom (or (:team2-score game) 0))]
    (fn []
      [:tr
       [:td (:time game)]
       [:td (str (get-in game [:team1 :player1]) " / "
                 (get-in game [:team1 :player2]))]
       [:td (str (get-in game [:team2 :player1]) " / "
                 (get-in game [:team2 :player2]))]
       [:td
        [:input.score-input
         {:type "number"
          :value @team1-score
          :on-change #(reset! team1-score (.. % -target -value))}]]
       [:td
        [:input.score-input
         {:type "number"
          :value @team2-score
          :on-change #(reset! team2-score (.. % -target -value))}]]
       [:td
        [:button.btn-secondary
         {:on-click #(rf/dispatch
                      [:submit-game-result
                       (:id game)
                       (js/parseInt @team1-score)
                       (js/parseInt @team2-score)])}
         "Save"]]])))

(defn game-list []
  (let [games @(rf/subscribe [:games])]
    [:div
     [:div.header-bar
      [:h2 "Today's Games"]
      ;; Print button
      [:button.btn-secondary
       {:on-click #(js/window.print)}
       "Print Game List"]
      ;; Generate next session
      [:button.btn-primary
       {:on-click #(rf/dispatch [:generate-next-session])}
       "Generate Next Session"]]
     (if (seq games)
       [:table
        [:thead
         [:tr
          [:th "Time"]
          [:th "Team 1"]
          [:th "Team 2"]
          [:th "Score T1"]
          [:th "Score T2"]
          [:th "Action"]]]
        [:tbody
         (for [g games]
           ^{:key (:id g)}
           [game-row g])]]
       [:p "No games scheduled yet. Click 'Generate Next Session' to create matchups."])]))

(defn home-panel []
  (let [user @(rf/subscribe [:user])]
    [:div
     [:h3 "Welcome!"]
     [:p (str "Hello, " (or (:email user) "anonymous user"))]
     [game-list]]))

(defn main-panel []
  (let [user @(rf/subscribe [:user])]
    [:div
     (if user
       [home-panel]
       [login-panel])]))
