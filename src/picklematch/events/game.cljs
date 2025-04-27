(ns picklematch.events.game
  (:require
   [re-frame.core :as rf]
   [clojure.string :as str]
   [picklematch.firebase.firestore :as fbf]
   [picklematch.firebase.location :as location]
   [picklematch.rating :as rating]))

;; Schedule a game
(rf/reg-event-fx
 :schedule-game
 (fn [{:keys [db]} [_ date-str time-str location-id]]
   (when (and (seq (str/trim date-str))
              (seq (str/trim time-str))
              (seq (str/trim location-id)))
     {:db (assoc db :loading? true)
      :firebase/add-game [date-str time-str location-id]})))

(rf/reg-fx
 :firebase/add-game
 (fn [[date-str time-str location-id]]
   (fbf/add-game!
    date-str time-str location-id
    (fn [game-id]
      (rf/dispatch [:load-games-for-date date-str]))
    (fn [err]
      (js/console.error "Error scheduling game:" err)
      (rf/dispatch [:games-loaded nil])))))

;; Helper to format Date object to YYYY-MM-DD based on local time
(defn format-date-obj-to-iso-str [date-obj]
  (when date-obj
    (let [year (.getFullYear date-obj)
          month-raw (inc (.getMonth date-obj)) ; Month is 0-indexed
          day-raw (.getDate date-obj)
          month (if (< month-raw 10) (str "0" month-raw) (str month-raw))
          day (if (< day-raw 10) (str "0" day-raw) (str day-raw))]
      (str year "-" month "-" day))))

;; Load games for a date
(rf/reg-event-db
 :set-selected-date
 (fn [db [_ date-obj]]
   (assoc db :selected-date date-obj)))

(rf/reg-event-fx
 :load-games-for-date
 (fn [{:keys [db]} [_ date-str]]
   {:db (assoc db :loading? true)
    :firebase/load-games-for-date [date-str true]}))

(rf/reg-fx
 :firebase/load-games-for-date
 (fn [[date-str _dispatch?]]
   (fbf/load-games-for-date!
    date-str
    (fn [games]
      (rf/dispatch [:games-loaded games]))
    (fn [err]
      (js/console.error "Error loading games:" err)
      (rf/dispatch [:games-loaded []])))))

(rf/reg-event-db
 :games-loaded
 (fn [db [_ games]]
   (-> db
       (assoc :loading? false)
       (assoc :games games))))

;; Register for a game
(rf/reg-event-fx
 :register-for-game
 (fn [{:keys [db]} [_ game-id team-key]]
   (let [uid   (get-in db [:user :uid])
         email (get-in db [:user :email])]
     (when uid
       {:db (assoc db :loading? true)
        :firebase/register-for-game [game-id team-key uid email]}))))

(rf/reg-fx
 :firebase/register-for-game
 (fn [[game-id team-key uid email]]
   (fbf/register-for-game!
    game-id team-key uid email
    (fn []
      (rf/dispatch [:reload-current-date-games]))
    (fn [err]
      (js/console.error "Error registering for game:" err)
      (rf/dispatch [:games-loaded nil])))))

(rf/reg-event-fx
 :reload-current-date-games
 (fn [{:keys [db]} _]
   (let [date-obj (:selected-date db)
         date-str (format-date-obj-to-iso-str date-obj)] ; Use local date formatting
     (when date-str ; Ensure date-str is not nil
       {:dispatch [:load-games-for-date date-str]}))))

;; Submit game result => update ratings
(rf/reg-event-fx
 :submit-game-result
 (fn [{:keys [db]} [_ game-id team1-score1 team1-score2 team2-score1 team2-score2]]
   (let [games (:games db)
         game  (some #(when (= (:id %) game-id) %) games)
         updated-game (assoc game 
                             :team1-score1 team1-score1 
                             :team1-score2 team1-score2
                             :team2-score1 team2-score1
                             :team2-score2 team2-score2)
         new-db (update db :games
                        (fn [gs]
                          (map (fn [g] (if (= (:id g) game-id) updated-game g))
                               gs)))]
     (let [[updated-ratings rating-event]
           (rating/recalculate-ratings new-db updated-game)]
       {:db (-> new-db
                (assoc :players updated-ratings)
                (update :log-of-rating-events conj rating-event))
        :firebase/store-game-score [updated-game]}))))

(rf/reg-fx
 :firebase/store-game-score
 (fn [[updated-game]]
   (fbf/store-game-score! updated-game)))

;; Load all users
(rf/reg-event-fx
 :load-all-users
 (fn [{:keys [db]} _]
   {:db db
    :firebase/load-all-users true}))

(rf/reg-fx
 :firebase/load-all-users
 (fn [_]
   (fbf/load-all-users!
    (fn [users-map]
      (rf/dispatch [:all-users-loaded users-map]))
    (fn [err]
      (js/console.error "Error loading all users" err)
      (rf/dispatch [:all-users-loaded {}])))))

(rf/reg-event-db
 :all-users-loaded
 (fn [db [_ users-map]]
   (assoc db :players users-map)))

;; Load all game dates
(rf/reg-event-fx
 :load-all-game-dates
 (fn [{:keys [db]} _]
   {:db (assoc db :loading? true)
    :firebase/load-all-game-dates true}))

(rf/reg-fx
 :firebase/load-all-game-dates
 (fn [_]
   (fbf/load-all-game-dates!
    (fn [dates]
      (rf/dispatch [:all-game-dates-loaded dates]))
    (fn [err]
      (js/console.error "Error loading game dates:" err)
      (rf/dispatch [:all-game-dates-loaded #{}])))))

(rf/reg-event-db
 :all-game-dates-loaded
 (fn [db [_ dates]]
   (-> db
       (assoc :loading? false)
       (assoc :all-game-dates (set dates)))))

 ;; Auto-Assign Example
 (rf/reg-event-fx
  :auto-assign-players
  (fn [{:keys [db]} [_ date-str times]]
    (let [players (->> (vals (:players db))
                       (sort-by :rating >))
          groups  (partition 4 4 nil players)
          num-games-to-create (min (count times) (count groups))]
      (if (pos? num-games-to-create)
        {:db (-> db ; Set loading and initialize counter
                 (assoc :loading? true)
                 (assoc :auto-assign-pending-count num-games-to-create))
         :dispatch-n ; Dispatch creation events
         (map-indexed
          (fn [idx time]
            [:create-game-with-players date-str time (nth groups idx)])
          (take num-games-to-create times))}
        {:db db})))) ; Do nothing if no games to create

(rf/reg-event-fx
 :create-game-with-players
 (fn [{:keys [db]} [_ date-str time-str group-of-4]]
   (let [[p1 p2 p3 p4] (map :uid group-of-4)
         selected-location-id (or (:selected-location-id db) 
                                  (when-let [locations (:locations db)]
                                    (when-let [first-loc (first locations)]
                                      (:id first-loc)))) ; Use selected location ID or first location ID
         doc-data {:date date-str
                   :time time-str
                   :location-id selected-location-id ; Use selected location ID
                   :team1 {:player1 p1 :player2 p2}
                   :team2 {:player1 p3 :player2 p4}
                   :team1-score1 0
                   :team1-score2 0
                    :team2-score1 0
                    :team2-score2 0}]
      ;; Don't set loading here, it's set by the caller :auto-assign-players
      {:firebase/add-game-with-players doc-data})))

(rf/reg-fx
 :firebase/add-game-with-players
 (fn [doc-data]
  (fbf/add-game-with-players!
   doc-data
   (fn [doc-id]
     (rf/dispatch [:auto-assign-game-creation-success doc-id])) ; Dispatch success
   (fn [err]
     (js/console.error "Error auto-assigning game" err)
     (rf/dispatch [:auto-assign-game-creation-failure err]))))) ; Dispatch failure

;; Handlers to track completion of auto-assign
(rf/reg-event-fx
 :auto-assign-game-creation-success
 (fn [{:keys [db]} [_ game-id]]
   (let [new-count (dec (:auto-assign-pending-count db 1))] ; Decrement counter (default to 1 if missing)
     (if (zero? new-count)
       {:db (-> db ; Last game created: clear loading, remove counter
                (assoc :loading? false)
                (dissoc :auto-assign-pending-count)
                (update :newly-created-games conj game-id))
        :dispatch [:reload-current-date-games]} ; Reload games
       {:db (-> db ; More games pending: update counter
                (assoc :auto-assign-pending-count new-count)
                (update :newly-created-games conj game-id))}))))

(rf/reg-event-fx
 :auto-assign-game-creation-failure
 (fn [{:keys [db]} [_ error]]
   (js/console.error "Auto-assign game creation failed:" error)
   (let [new-count (dec (:auto-assign-pending-count db 1))]
     (if (zero? new-count)
       {:db (-> db ; Last operation failed: clear loading, remove counter
                (assoc :loading? false)
                (dissoc :auto-assign-pending-count))}
       {:db (assoc db :auto-assign-pending-count new-count)})))) ; More games pending: update counter

;; Delete a game (Admin only)
(rf/reg-event-fx
 :delete-game
 (fn [{:keys [db]} [_ game-id]]
   ;; TODO: Add admin check here? Or rely on UI? Relying on UI for now.
   {:db (assoc db :loading? true)
    :firebase/delete-game game-id}))

(rf/reg-fx
 :firebase/delete-game
 (fn [game-id]
   (fbf/delete-game!
    game-id
    (fn []
      (js/console.log "Game deleted successfully:" game-id)
      (rf/dispatch [:reload-current-date-games])) ; Reload games for the current date
    (fn [err]
      (js/console.error "Error deleting game:" err)
      (rf/dispatch [:games-loaded nil]))))) ; Clear loading state on error

;; Set selected location
(rf/reg-event-db
 :set-selected-location
 (fn [db [_ location-id]]
   (assoc db :selected-location-id location-id)))
