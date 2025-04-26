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
        team2-score (r/atom (:team2-score game))
        is-admin?   (rf/subscribe [:is-admin?])] ; Subscribe to admin status
    (fn []
      (let [{:keys [id time team1 team2]} game
            t1p1 (or (:player1 team1) "Empty")
            t1p2 (or (:player2 team1) "Empty")
            t2p1 (or (:player1 team2) "Empty")
            t2p2 (or (:player2 team2) "Empty")] ; <-- Corrected: Bindings vector closes here
        ;; Body of the inner let starts here
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
           "Join Team2"]]
         [:td ; New cell for Admin Actions
          (when @is-admin? ; Only show if user is admin
            [:button.btn-danger ; Use danger class for delete
             {:on-click #(if (js/confirm (str "Are you sure you want to delete the game at " time "?"))
                           (rf/dispatch [:delete-game id]))} ; Add confirmation
             "Delete"])]
         ] ; End of :tr vector
       )) ; <-- Closing parenthesis for inner 'let'
     ) ; <-- Add closing parenthesis for anonymous function (fn [])
   ) ; <-- Add closing parenthesis for outer let

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
         [:th "Register"]
         [:th "Admin Action"]]] ; Add header for the new column
      [:tbody
       (for [g games]
         ^{:key (:id g)} [game-row g])]]]))
