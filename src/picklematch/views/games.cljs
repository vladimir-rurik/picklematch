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
  (let [team1-score1 (r/atom (:team1-score1 game))
        team1-score2 (r/atom (:team1-score2 game))
        team2-score1 (r/atom (:team2-score1 game))
        team2-score2 (r/atom (:team2-score2 game))
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
         [:td ; Score T1 (2 rows)
          [:div.score-row
           [:input.score-input
            {:type "number"
             :value @team1-score1
             :placeholder "Game 1"
             :on-change #(reset! team1-score1 (.. % -target -value))}]
           [:input.score-input
            {:type "number"
             :value @team1-score2
             :placeholder "Game 2"
             :on-change #(reset! team1-score2 (.. % -target -value))}]]
         ]
         [:td ; Score T2 (2 rows)
          [:div.score-row
           [:input.score-input
            {:type "number"
             :value @team2-score1
             :placeholder "Game 1"
             :on-change #(reset! team2-score1 (.. % -target -value))}]
           [:input.score-input
            {:type "number"
             :value @team2-score2
             :placeholder "Game 2"
             :on-change #(reset! team2-score2 (.. % -target -value))}]]
         ]
         [:td ; Team 2 
          (str (or (user-display t2p1) "Empty")
               " / "
               (or (user-display t2p2) "Empty"))]
         [:td {:class "no-print"} ; Hide Action column cell
          [:button.btn-secondary
           {:on-click #(rf/dispatch
                        [:submit-game-result
                         id
                         (js/parseInt @team1-score1)
                         (js/parseInt @team1-score2)
                         (js/parseInt @team2-score1)
                         (js/parseInt @team2-score2)])}
           "Save"]]
         [:td {:class "no-print"} ; Hide Register column cell
          [:button.btn-primary
           {:on-click #(rf/dispatch [:register-for-game id :team1])}
           "Join Team1"]
          [:button.btn-primary
           {:on-click #(rf/dispatch [:register-for-game id :team2])}
           "Join Team2"]]
         [:td {:class "no-print"} ; Hide Admin Action column cell
          (when @is-admin? ; Only show if user is admin
            [:button.btn-danger ; Use danger class for delete
             {:on-click #(if (js/confirm (str "Are you sure you want to delete the game at " time "?"))
                           (rf/dispatch [:delete-game id]))} ; Add confirmation
             "Delete"])]
         ] ; End of :tr vector
       )) ; <-- Closing parenthesis for inner 'let'
     ) ; <-- Add closing parenthesis for anonymous function (fn [])
   ) ; <-- Add closing parenthesis for outer let

(defn format-date-iso [date-obj]
  (when date-obj
    (let [year (.getFullYear date-obj)
          month-raw (inc (.getMonth date-obj)) ; Month is 0-indexed
          day-raw (.getDate date-obj)
          month (if (< month-raw 10) (str "0" month-raw) (str month-raw)) ; Manual padding
          day (if (< day-raw 10) (str "0" day-raw) (str day-raw))] ; Manual padding
      (str year "-" month "-" day))))

(defn game-list []
  (let [games @(rf/subscribe [:games])
        selected-date @(rf/subscribe [:selected-date]) ; Subscribe to selected date
        formatted-date (format-date-iso selected-date)]
    [:div#game-list-container ; Add ID for print targeting
     [:div.header-bar
      [:h2 (str "Game List" (when formatted-date (str " for " formatted-date)))] ; Update title
      [:button.btn-secondary.no-print {:on-click #(js/window.print)} ; Add no-print class to button
       "Print Game List"]]
     [:table
      [:thead
       [:tr
        [:th "Time"]
        [:th "Team 1"]
         [:th "Score T1"]
         [:th "Score T2"]
         [:th "Team 2"] ; Moved Header
         [:th {:class "no-print"} "Action"] ; Hide Action header
         [:th {:class "no-print"} "Register"] ; Hide Register header
         [:th {:class "no-print"} "Admin Action"]]] ; Hide Admin Action header
      [:tbody
       (for [g games]
         ^{:key (:id g)} [game-row g])]]]))
