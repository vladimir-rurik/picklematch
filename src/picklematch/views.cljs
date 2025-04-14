(ns picklematch.views
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]))

(defn login-panel []
  [:div.login
   [:button {:on-click #(rf/dispatch [:sign-in-with-google])}
    "Sign in with Google"]
   ;; Similarly for Facebook
   [:button {:on-click #(js/alert "Facebook sign-in not yet wired")}
    "Sign in with Facebook"]])

(defn game-row [game]
  (let [team1-score (r/atom (or (:team1-score game) 0))
        team2-score (r/atom (or (:team2-score game) 0))]
    (fn []
      [:tr
       [:td (str (:time game))]
       [:td (str (get-in game [:team1 :player1]) " / "
                 (get-in game [:team1 :player2]))]
       [:td (str (get-in game [:team2 :player1]) " / "
                 (get-in game [:team2 :player2]))]
       [:td
        [:input {:type "number"
                 :value @team1-score
                 :on-change #(reset! team1-score (.. % -target -value))}]]
       [:td
        [:input {:type "number"
                 :value @team2-score
                 :on-change #(reset! team2-score (.. % -target -value))}]]
       [:td
        [:button
         {:on-click #(rf/dispatch [:submit-game-result
                                   (:id game)
                                   (int @team1-score)
                                   (int @team2-score)])}
         "Save"]]])))

(defn game-list []
  (let [games @(rf/subscribe [:games])]
    [:table
     [:thead
      [:tr
       [:th "Time"]
       [:th "Team 1"]
       [:th "Team 2"]
       [:th "Score T1"]
       [:th "Score T2"]
       [:th ""]]]
     [:tbody
      (for [g games]
        ^{:key (:id g)} [game-row g])]]))

(defn main-panel []
  (let [user @(rf/subscribe [:user])]
    [:div
     (if user
       [:div
        [:h2 "Welcome " (:email user)]
        [game-list]]
       [login-panel])]))