(ns picklematch.core
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   ["react-dom/client" :as rdom-client]
   [picklematch.views :refer [main-panel]]
   [picklematch.events]   ;; must be required for side effects
   [picklematch.subs]     ;; must be required so subscriptions are registered
   [picklematch.firebase :refer [auth-inst]]
   ["firebase/auth" :refer [onAuthStateChanged]]))

;; We'll watch for Firebase auth changes
(rf/reg-event-fx
 :handle-auth-changed
 (fn [{:keys [db]} [_ user]]
   (if user
     (let [uid (.-uid user)
           email (.-email user)]
       {:dispatch
        [:login-success {:uid    uid
                         :email  email
                         :rating 1200}]})
     ;; No user? Go to login page
     {:db (assoc db :user nil :page :login)})))

(defn listen-for-auth-changes []
  (onAuthStateChanged
   auth-inst
   (fn [user]
     (rf/dispatch [:handle-auth-changed user]))))

;; React 18 createRoot
(defonce root
  (rdom-client/createRoot (js/document.getElementById "app")))

(defn ^:dev/after-load mount-root []
  (.render root (r/as-element [main-panel])))

(defn init []
  (rf/dispatch-sync [:initialize])
  (listen-for-auth-changes)
  (mount-root))
