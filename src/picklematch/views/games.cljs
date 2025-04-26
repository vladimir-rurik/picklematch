(ns picklematch.views.games
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [clojure.string :as str]))

(defn user-display [uid]
  (let [players @(rf/subscribe [:players])
        user-map (get players uid)
        email    (:email user-map)]
    (some-> email
            (str/split #"@")
            first)))

(defn game-row [game]
  (let [team1-score (r/atom (:team1-score game))
        team2-score (r/atom (:team2-score game))]
    (fn []
      (let [{:keys [id time team1 team2]} game
            t1p1 (or (:player1 team1) "Empty")
            t1p2 (or (:player2 team1) "Empty")
            t2p1 (or (:player1 team2) "Empty")
            t2p2 (or (:player2 team2) "Empty")]
        [:tr
         [:td time]
         [:td (str (or (user-display t1p1) "Empty")
                   " / "
                   (or (user-display t1p2) "Empty"))]
         [:td (str (or (user-display t2p1) "Empty")
                   " / "
                   (or (user-display t2p2) "Empty"))]
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
                         id
                         (js/parseInt @team1-score)
                         (js/parseInt @team2-score)])}
           "Save"]]
         [:td
          [:button.btn-primary
           {:on-click #(rf/dispatch [:register-for-game id :team1])}
           "Join Team1"]
          [:button.btn-primary
           {:on-click #(rf/dispatch [:register-for-game id :team2])}
           "Join Team2"]]]))))

(defn game-list []
  (let [games @(rf/subscribe [:games])]
    [:div
     [:div.header-bar
      [:h2 "Game List"]
      [:button.btn-secondary {:on-click #(js/window.print)}
       "Print Game List"]]
     [:table
      [:thead
       [:tr
        [:th "Time"]
        [:th "Team 1"]
        [:th "Team 2"]
        [:th "Score T1"]
        [:th "Score T2"]
        [:th "Action"]
        [:th "Register"]]]
      [:tbody
       (for [g games]
         ^{:key (:id g)} [game-row g])]]]))