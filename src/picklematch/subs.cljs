(ns picklematch.subs
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub :user (fn [db _] (:user db)))
(rf/reg-sub :games (fn [db _] (:games db)))
(rf/reg-sub :players (fn [db _] (:players db)))
(rf/reg-sub :selected-date (fn [db _] (:selected-date db)))
(rf/reg-sub :loading? (fn [db _] (:loading? db)))
(rf/reg-sub :all-game-dates (fn [db _] (:all-game-dates db)))
(rf/reg-sub :auth-error (fn [db _] (:auth-error db)))
(rf/reg-sub :auth-message (fn [db _] (:auth-message db)))
(rf/reg-sub :locations (fn [db _] (:locations db)))
(rf/reg-sub :selected-location-id (fn [db _] (:selected-location-id db)))

;; Selected location subscription
(rf/reg-sub
 :selected-location
 :<- [:locations]
 :<- [:selected-location-id]
 (fn [[locations selected-location-id] _]
   (if (and locations selected-location-id)
     (first (filter #(= (:id %) selected-location-id) locations))
     nil)))

;; Filtered games subscription
(rf/reg-sub
 :filtered-games
 :<- [:games]
 :<- [:selected-location-id]
 (fn [[games selected-location-id] _]
   (if selected-location-id
     (filter #(= (:location-id %) selected-location-id) games)
     games)))

(rf/reg-sub
 :is-admin?
 :<- [:user]
 (fn [user _]
    (= (:role user) "admin")))

;; Game dates for the currently selected location
(rf/reg-sub :game-dates-for-location (fn [db _] (:game-dates-for-location db)))
