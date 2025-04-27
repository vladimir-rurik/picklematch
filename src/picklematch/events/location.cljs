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

(rf/reg-event-db
 :locations-loaded
 (fn [db [_ locations]]
   (let [first-location (first locations)
         first-location-id (when first-location (:id first-location))]
     (-> db
         (assoc :loading? false)
         (assoc :locations locations)
         ;; If no location is selected, select the first one
         (update :selected-location-id #(or % first-location-id))))))

;; Initialize default locations
(rf/reg-event-fx
 :initialize-default-locations
 (fn [_ _]
   {:firebase/initialize-default-locations true}))

(rf/reg-fx
 :firebase/initialize-default-locations
 (fn [_]
   (location/initialize-default-locations!)))
