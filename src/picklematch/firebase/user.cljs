(ns picklematch.firebase.user
  (:require
   [picklematch.firebase.init :refer [db]]
   ["firebase/firestore" :as firestore]))

;; User-specific Firestore operations
(defn store-user-if-new!
  "Create user doc if not already present, with optional :active setting."
  ([uid email]
   (store-user-if-new! uid email true))
  ([uid email active?]
   (let [doc-ref (firestore/doc (firestore/collection db "users") uid)]
     (-> (firestore/getDoc doc-ref)
         (.then
          (fn [snapshot]
            (if-not (.exists snapshot)
              (-> (firestore/setDoc doc-ref
                                    (clj->js {:uid uid
                                              :email email
                                              :rating 1000
                                              :role "ordinary"
                                              :active active?}))
                  (.then #(js/console.log "New user doc created for" uid))
                  (.catch #(js/console.error "Error creating user doc:" %)))
              (js/console.log "User doc already exists for" uid))))
         (.catch #(js/console.error "Error checking user in Firestore:" %))))))

(defn load-user-doc! [uid on-success on-fail]
  (let [doc-ref (firestore/doc (firestore/collection db "users") uid)]
    (-> (firestore/getDoc doc-ref)
        (.then (fn [snapshot]
                 (if (.-exists snapshot)
                   (on-success (js->clj (.data snapshot) :keywordize-keys true))
                   (on-success nil))))
        (.catch on-fail))))

(defn update-user-role! [uid new-role]
  (let [doc-ref (firestore/doc (firestore/collection db "users") uid)]
    (-> (firestore/updateDoc doc-ref #js {:role new-role})
        (.then #(js/console.log (str "Updated user " uid " role to " new-role)))
        (.catch #(js/console.error "Error updating role:" %)))))

(defn update-user-rating! [uid new-rating]
  (let [doc-ref (firestore/doc (firestore/collection db "users") uid)]
    (-> (firestore/updateDoc doc-ref #js {:rating new-rating})
        (.then #(js/console.log "Updated user rating:" uid new-rating))
        (.catch #(js/console.error "Error updating user rating:" %)))))

(defn load-all-users! [on-success on-fail]
  (let [col-ref (firestore/collection db "users")]
    (-> (firestore/getDocs col-ref)
        (.then (fn [snapshot]
                 (let [docs  (.-docs snapshot)
                       users (into {}
                                   (map (fn [doc]
                                          (let [udata (js->clj (.data doc) :keywordize-keys true)
                                                uid   (:uid udata)]
                                            [uid udata])))
                                   docs)]
                   (on-success users))))
        (.catch on-fail))))

(defn set-user-active! [uid active?]
  (let [doc-ref (firestore/doc (firestore/collection db "users") uid)]
    (-> (firestore/updateDoc doc-ref #js {:active active?})
        (.then #(js/console.log "Set user doc: active=" active?))
        (.catch #(js/console.error "Error setting user doc active:" %)))))
