(ns picklematch.core
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   ["react-dom/client" :as rdom-client]
   ;; Our firebase references
   [picklematch.firebase :refer [auth-inst]]
   ;; We need signInAnonymously for anon, plus GoogleAuthProvider for Google
   ["firebase/auth" :refer [signInAnonymously
                            GoogleAuthProvider
                            signInWithPopup]]))

;; -- Events & Effects --

(rf/reg-event-db
 :initialize
 (fn [_ _]
   {:user nil}))

;; Anonymous Sign-In
(rf/reg-event-fx
 :sign-in-anonymous
 (fn [{:keys [db]} _]
   {:db db
    :sign-in/anonymous true}))

(rf/reg-fx
 :sign-in/anonymous
 (fn [_]
   (-> (signInAnonymously auth-inst)
       (.then (fn [cred]
                (js/console.log "Anon sign-in success" cred)
                (rf/dispatch [:login-success (.-user cred)])))
       (.catch (fn [err]
                 (js/console.error "Anon sign-in error" err))))))

;; Google Sign-In
(rf/reg-event-fx
 :sign-in-google
 (fn [{:keys [db]} _]
   {:db db
    :sign-in/google true}))

(rf/reg-fx
 :sign-in/google
 (fn [_]
   (let [provider (GoogleAuthProvider.)]
     (-> (signInWithPopup auth-inst provider)
         (.then (fn [result]
                  (js/console.log "Google sign-in success" result)
                  (rf/dispatch [:login-success (.-user result)])))
         (.catch (fn [error]
                   (js/console.error "Google sign-in error" error)))))))

(rf/reg-event-db
 :login-success
 (fn [db [_ user-obj]]
   (assoc db :user
          {:uid          (.-uid user-obj)
           :email        (.-email user-obj)
           :display-name (.-displayName user-obj)
           :photo        (.-photoURL user-obj)})))

(rf/reg-sub
 :user
 (fn [db _] (:user db)))

;; -- Views --

(defn login-panel []
  [:div
   [:h2 "PickleMatch Login"]
   [:button {:style {:margin-right "1rem"}
             :on-click #(rf/dispatch [:sign-in-google])}
    "Sign In with Google"]
   [:button {:on-click #(rf/dispatch [:sign-in-anonymous])}
    "Sign In Anonymously"]])

(defn home-panel []
  (let [user @(rf/subscribe [:user])]
    [:div
     [:h2 "Welcome!"]
     [:p (str "Hello, " (or (:display-name user) "anonymous user"))]
     [:p (str "Email: " (:email user))]
     (when (:photo user)
       [:img {:src (:photo user)
              :style {:width "60px"
                      :border-radius "50%"}}])]))

(defn main-panel []
  (let [user @(rf/subscribe [:user])]
    (if user
      [home-panel]
      [login-panel])))

;; -- React 18 Mount --

(defonce root
  (rdom-client/createRoot (js/document.getElementById "app")))

(defn ^:dev/after-load mount-root []
  (.render root (r/as-element [main-panel])))

(defn init []
  (rf/dispatch-sync [:initialize])
  (mount-root))
