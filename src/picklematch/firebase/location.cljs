(ns picklematch.firebase.location
  (:require
   [picklematch.firebase.init :refer [db]]
   ["firebase/firestore" :as firestore]))

;; Location-specific Firestore operations
(defn add-location! [name on-success on-fail]
  (let [locations-collection (firestore/collection db "locations")
        doc-data {:name name}]
    (-> (firestore/addDoc locations-collection (clj->js doc-data))
        (.then (fn [doc-ref] (on-success (.-id doc-ref))))
        (.catch on-fail))))

(defn load-all-locations! [on-success on-fail]
  (let [locations-collection (firestore/collection db "locations")]
    (-> (firestore/getDocs locations-collection)
        (.then (fn [query-snapshot]
                 (let [locations (for [doc (.-docs query-snapshot)]
                                   (assoc (js->clj (.data doc) :keywordize-keys true)
                                          :id (.-id doc)))]
                   (on-success (vec locations)))))
        (.catch on-fail))))

(defn get-location-by-id! [location-id on-success on-fail]
  (let [doc-ref (firestore/doc (firestore/collection db "locations") location-id)]
    (-> (firestore/getDoc doc-ref)
        (.then (fn [doc-snapshot]
                 (if (.exists doc-snapshot)
                   (on-success (assoc (js->clj (.data doc-snapshot) :keywordize-keys true)
                                      :id (.-id doc-snapshot)))
                   (on-success nil))))
        (.catch on-fail))))

;; Initialize default locations if they don't exist
(defn initialize-default-locations! []
  (let [default-locations ["Tondiraba Indoor" "Tondiraba Outdoor" "Koorti" "Golden Club" "Pirita"]]
    (load-all-locations!
     (fn [existing-locations]
       (let [existing-names (set (map :name existing-locations))]
         (doseq [loc-name default-locations]
           (when-not (contains? existing-names loc-name)
             (add-location! 
              loc-name
              (fn [_] (js/console.log "Added default location:" loc-name))
              (fn [err] (js/console.error "Error adding default location:" loc-name err)))))))
     (fn [err]
       (js/console.error "Error loading locations for initialization:" err)))))