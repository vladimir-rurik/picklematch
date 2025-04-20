(ns picklematch.views
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [clojure.string :as str]
   [picklematch.component.daypicker :refer [daypicker-component]]))

;; --------------------
;; Components
;; --------------------
(defn login-panel []
  [:div.login
   [:h2 "PickleMatch Login"]
   [:button.btn-primary
    {:on-click #(rf/dispatch [:sign-in-with-google])}
    "Sign in with Google"]
   ;;  [:button.btn-primary
   ;;   {:on-click #(rf/dispatch [:sign-in-with-facebook])}
   ;;   "Sign in with Facebook"]
   ])

(defn schedule-game-panel []
  (let [date-str (r/atom "")
        time-str (r/atom "")]
    (fn []
      [:div
       [:h3 "Schedule a new game"]
       [:label "Date: "]
       [:input {:type "date"
                :on-change #(reset! date-str (.. % -target -value))}]
       [:br]
       [:label "Time: "]
       [:input {:type "text"
                :placeholder "e.g. 8:00 AM"
                :on-change #(reset! time-str (.. % -target -value))}]
       [:button.btn-secondary
        {:on-click #(rf/dispatch [:schedule-game @date-str @time-str])}
        "Add Game"]])))

(defn date-selector []
  (let [selected-date @(rf/subscribe [:selected-date])]
    (fn []
      [:div.calendar
       [:h3 "Pick a date to view games"]
       [:input {:type "date"
                :value (some-> selected-date
                               (.toISOString)
                               (subs 0 10))
                :on-change (fn [e]
                             (let [val (.-value (.-target e))
                                   date-obj (js/Date. val)]
                               (rf/dispatch [:set-selected-date date-obj])
                               (rf/dispatch [:load-games-for-date val])))}]])))

(defn user-display
  "Given a uid, return a friendly display name from the user's email."
  [uid]
  (let [players @(rf/subscribe [:players]) ; {uid {:email ... :rating ...}}
        user-map (get players uid)
        email    (:email user-map)]
    ;; (some-> email (str/split #"\.") first)
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
          ;; For user registration: choose which side to join
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
      [:button.btn-secondary
       {:on-click #(js/window.print)}
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

(defn admin-toggle-role-button []
  [:button.btn-secondary
   {:on-click #(rf/dispatch [:toggle-admin-role])}
   "Toggle Admin Role"])

(defn admin-panel []
  [:div
   ;;  [:button.btn-secondary
   ;;   {:on-click #(rf/dispatch [:toggle-admin-role])}
   ;;   "Toggle Admin Role"]
   [schedule-game-panel]])

(defn home-panel []
  (let [user @(rf/subscribe [:user])
        role (:role user)
        email (:email user)
        first_name (some-> email (str/split #"@") first) ;; #"\." to split email into parts
        players   @(rf/subscribe [:players])
        uid       (:uid user)
        ;; Get the user's rating from the players map
        rating    (get-in players [uid :rating])]
    (js/console.log "User role:" role)
    (js/console.log "User rating:" rating)
    [:div
     [:div.header-bar
      [:h2 (str "Welcome, " (or first_name "anonymous") (when rating (str " (" rating ")")))]
      ;; Logout button
      [:button.btn-secondary
       {:on-click #(rf/dispatch [:logout])}
       "Logout"]]
    ;;  [date-selector]
     [daypicker-component]
     (when (= role "admin")
       [admin-panel])
     [game-list]]))

(defn main-panel []
  (let [user @(rf/subscribe [:user])
        loading? @(rf/subscribe [:loading?])]
    [:div
     (if loading?
       [:div "Loading..."]
       (if user
         [home-panel]
         [login-panel]))]))
