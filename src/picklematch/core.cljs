(ns picklematch.core
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   ["react-dom/client" :as rdom-client]
   [picklematch.views :refer [main-panel]]
   [picklematch.events]  ;; load to register events
   [picklematch.subs]    ;; load to register subs
   [picklematch.firebase :refer [auth-inst]]
   ["firebase/auth" :refer [onAuthStateChanged]]))

;; Event to handle authentication state change
(rf/reg-event-fx
 :handle-auth-changed
 (fn [{:keys [db]} [_ user]]
   (if user
     (let [uid (.-uid user)
           email (.-email user)]
       {:dispatch [:login-success {:uid uid :email email}]})
     {:db (assoc db :user nil)})))

(defn listen-for-auth-changes []
  (onAuthStateChanged
   auth-inst
   (fn [user]
     (rf/dispatch [:handle-auth-changed user]))))

;; React 18 mount
(defonce root
  (rdom-client/createRoot (js/document.getElementById "app")))

(defn ^:dev/after-load mount-root []
  (.render root (r/as-element [main-panel])))

(defn init []
  (rf/dispatch-sync [:initialize])
  (listen-for-auth-changes)
  (mount-root))
