(ns picklematch.events.location
  (:require
   [re-frame.core :as rf]
   [picklematch.firebase.location :as location]))

;; Load all locations
(rf/reg-event-fx
 :load-all-locations
 (fn [{:keys [db]} _]
   {:db (assoc db :loading? true)
    :firebase/load-all-locations true}))

(rf/reg-fx
 :firebase/load-all-locations
 (fn [_]
   (location/load-all-locations!
    (fn [locations]
      (rf/dispatch [:locations-loaded locations]))
    (fn [err]
      (js/console.error "Error loading locations:" err)
      (rf/dispatch [:locations-loaded []])))))

 (rf/reg-event-fx ; Change to -fx to allow dispatching
  :locations-loaded
  (fn [{:keys [db]} [_ locations]]
    (let [first-location (first locations)
          first-location-id (when first-location (:id first-location))
          selected-id (or (:selected-location-id db) first-location-id)]
      {:db (-> db
               (assoc :loading? false)
               (assoc :locations locations)
               ;; If no location is selected, select the first one
               (assoc :selected-location-id selected-id))
       ;; Load game dates for the selected location
       :dispatch (when selected-id [:load-game-dates-for-location selected-id])})))

;; Initialize default locations
(rf/reg-event-fx
 :initialize-default-locations
 (fn [_ _]
   {:firebase/initialize-default-locations true}))

(rf/reg-fx
 :firebase/initialize-default-locations
 (fn [_]
   (location/initialize-default-locations!)))
