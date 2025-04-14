(ns picklematch.core
  (:require
   [reagent.dom :as rdom]
   [re-frame.core :as rf]
    ;; Use the "auth-inst" from firebase.cljs:
   [picklematch.firebase :refer [auth-inst]]
    ;; If you want Firestore, also :refer [db]
    ;; For Google sign-in:
   ["firebase/auth" :refer [GoogleAuthProvider signInWithPopup FacebookAuthProvider]]))

;; ------------------------------------------------------
;; (A) Basic Re-frame Setup
;; ------------------------------------------------------

(rf/reg-event-db
 :initialize
 (fn [_ _]
   {:user nil}))

;; -- Google Sign-in
(rf/reg-event-fx
 :sign-in-with-google
 (fn [{:keys [db]} _]
   {:db db
    :sign-in/google true}))

(rf/reg-fx
 :sign-in/google
 (fn [_]
   (let [provider (GoogleAuthProvider.)]
     (-> (signInWithPopup auth-inst provider)
         (.then (fn [result]
                  ;; The user object is accessible at (.-user result)
                  (rf/dispatch [:login-success (.-user result)])))
         (.catch (fn [err]
                   (js/console.error "Google sign-in error: " err)))))))

;; -- Facebook Sign-in (optional)
(rf/reg-event-fx
 :sign-in-with-facebook
 (fn [{:keys [db]} _]
   {:db db
    :sign-in/facebook true}))

(rf/reg-fx
 :sign-in/facebook
 (fn [_]
   (let [provider (FacebookAuthProvider.)]
     (-> (signInWithPopup auth-inst provider)
         (.then (fn [result]
                  (rf/dispatch [:login-success (.-user result)])))
         (.catch (fn [err]
                   (js/console.error "Facebook sign-in error: " err)))))))

;; Store user info in app-db
(rf/reg-event-db
 :login-success
 (fn [db [_ user]]
   (assoc db :user
          {:uid          (.-uid user)
           :display-name (.-displayName user)
           :email        (.-email user)
           :photo        (.-photoURL user)})))

(rf/reg-sub
 :user
 (fn [db _]
   (:user db)))


;; ------------------------------------------------------
;; (B) Minimal UI
;; ------------------------------------------------------

(defn login-panel []
  [:div {:style {:margin "2rem"}}
   [:h2 "PickleMatch Login"]
   [:button {:style {:margin-right "1rem"}
             :on-click #(rf/dispatch [:sign-in-with-google])}
    "Sign in with Google"]
   [:button {:on-click #(rf/dispatch [:sign-in-with-facebook])}
    "Sign in with Facebook"]])

(defn home-panel []
  (let [user @(rf/subscribe [:user])]
    [:div {:style {:margin "2rem"}}
     [:h2 "Welcome!"]
     [:p (str "Hello, " (:display-name user))]
     [:p (str "Email: " (:email user))]
     (when-let [photo (:photo user)]
       [:img {:src photo
              :style {:width "80px"
                      :border-radius "50%"}}])]))

(defn main-panel []
  (let [user @(rf/subscribe [:user])]
    (if user
      [home-panel]
      [login-panel])))


;; ------------------------------------------------------
;; (C) The init function
;; ------------------------------------------------------

(defn ^:dev/after-load mount-root []
  (rdom/render [main-panel]
               (.getElementById js/document "app")))

(defn init []
  (rf/dispatch-sync [:initialize])
  (mount-root))