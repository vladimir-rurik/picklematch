(ns picklematch.views
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [picklematch.schedule :refer [generate-schedule]]))

;; --- LOGIN PANEL ---
(defn login-panel []
  [:div.login
   [:h2 "PickleMatch Login"]
   [:button.btn-primary
    {:on-click #(rf/dispatch [:sign-in-with-google])}
    "Sign in with Google"]
   [:button.btn-primary
    {:on-click #(rf/dispatch [:sign-in-with-facebook])}
    "Sign in with Facebook"]])

;; --- SCHEDULE PAGE ---
(defn schedule-page []
  (let [user @(rf/subscribe [:user])
        user-email (:email user)
        user-rating (or (:rating user) "N/A")

        ;; For demonstration, a static list of players:
        ;; In a real app, you'd load from Firestore or from (:players db).
        players ["Olga B" "Mikhail M" "Irina" "Oleg S"
                 "Mercedes" "Ramil" "Aleksei" "Merle"
                 "Erich" "Andrian"]

        ;; The sequence of courts (in the given order):
        court-seq [5 4 3 6]

        ;; Some example times for the day:
        times ["18:30" "18:53" "19:09" "19:25" "19:42" "20:00"]

        ;; Generate the schedule:
        {:keys [games waiting]} (generate-schedule players court-seq times)]

    [:div
     [:h2 "Today's Schedule"]
     [:p (str "User: " user-email " (Rating: " user-rating ")")]
     [:button.btn-secondary
      {:on-click #(js/window.print)}
      "Print Schedule"]

     ;; Group the games by :time for display
     (let [by-time (group-by :time games)
           sorted-times (sort (keys by-time))]
       (for [t sorted-times]
         (let [games-at-time (by-time t)]
           ^{:key t}
           [:div
            [:h4 (str "Game time: " t)]
            [:table
             [:thead
              [:tr
               [:th "Court"]
               [:th "Players"]]]
             [:tbody
              (for [{:keys [court players]} games-at-time]
                ^{:key (str t "-" court)}
                [:tr
                 [:td court]
                 [:td (apply str (interpose " / " players))]])]]])))

     (when (seq waiting)
       [:div
        [:h4 "Waiting Players"]
        [:ul
         (for [w waiting]
           ^{:key w} [:li w])]])]))

;; Example: if you want a game list with scoring:
(defn game-list []
  (let [games @(rf/subscribe [:games])]
    [:div
     [:h2 "Game List (Scoring Example)"]
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
         (let [team1-score (r/atom (or (:team1-score g) 0))
               team2-score (r/atom (or (:team2-score g) 0))]
           [:tr
            [:td (str (:time g))]
            [:td (str (get-in g [:team1 :player1]) " / "
                      (get-in g [:team1 :player2]))]
            [:td (str (get-in g [:team2 :player1]) " / "
                      (get-in g [:team2 :player2]))]
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
                             (:id g)
                             (js/parseInt @team1-score)
                             (js/parseInt @team2-score)])}
              "Save"]]]))]]]))

;; --- MAIN PANEL (Router) ---
(defn main-panel []
  (let [page @(rf/subscribe [:page])
        user @(rf/subscribe [:user])]
    [:div
     (case page
       :login     [login-panel]
       :schedule  [schedule-page]
       ;; You might still display the old game-list for testing:
       ;; :games     [game-list]
       ;; fallback
       [:div "404 - Not Found"])]))
