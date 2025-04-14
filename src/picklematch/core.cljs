(ns picklematch.core
  (:require
   [reagent.dom :as rdom]
   [re-frame.core :as rf]
   ;; Import Firebase modules from npm
   ["firebase/app" :as firebase]
   ["firebase/auth" :as auth]))

;; -------------------------------------------------------
;; 1) Firebase Config & Initialization
;; -------------------------------------------------------

(def firebase-config
  {:apiKey            "YOUR-API-KEY"
   :authDomain        "your-app.firebaseapp.com"
   :projectId         "your-app"
   :storageBucket     "your-app.appspot.com"
   :messagingSenderId "SENDER-ID"
   :appId             "APP-ID"})

;; We store the initialized app in an Atom or defonce to prevent re-init
(defonce fb-app
  (firebase/initializeApp (clj->js firebase-config)))

;; The Auth instance
(defonce fb-auth (auth/getAuth fb-app))


;; -------------------------------------------------------
;; 2) Re-frame: Events, Subscriptions
;; -------------------------------------------------------

(rf/reg-event-db
 :initialize
 (fn [_ _]
   ;; Initial state
   {:user nil}))

(rf/reg-event-fx
 :sign-in-with-google
 (fn [{:keys [db]} _]
   ;; Return an effect that triggers Google Popup sign-in
   {:db db
    :sign-in/google true}))

(rf/reg-fx
 :sign-in/google
 (fn [_]
   (let [provider (auth/GoogleAuthProvider.)]
     (-> (auth/signInWithPopup fb-auth provider)
         (.then (fn [result]
                  ;; once login is successful, dispatch event to store user in app-db
                  (rf/dispatch [:login-success (.-user result)])))
         (.catch (fn [error]
                   (js/console.error "Google sign-in error: " error)))))))

(rf/reg-event-fx
 :sign-in-with-facebook
 (fn [{:keys [db]} _]
   {:db db
    :sign-in/facebook true}))

(rf/reg-fx
 :sign-in/facebook
 (fn [_]
   (let [provider (auth/FacebookAuthProvider.)]
     (-> (auth/signInWithPopup fb-auth provider)
         (.then (fn [result]
                  (rf/dispatch [:login-success (.-user result)])))
         (.catch (fn [error]
                   (js/console.error "Facebook sign-in error: " error)))))))

(rf/reg-event-db
 :login-success
 (fn [db [_ user]]
   (let [uid          (.-uid user)
         email        (.-email user)
         display-name (.-displayName user)
         photo-url    (.-photoURL user)]
     (assoc db :user
            {:uid          uid
             :email        email
             :display-name display-name
             :photo-url    photo-url}))))

(rf/reg-sub
 :user
 (fn [db _]
   (:user db)))

;; -------------------------------------------------------
;; 3) Reagent Views
;; -------------------------------------------------------

(defn login-panel []
  [:div {:style {:margin "2rem"}}
   [:h1 "Welcome to PickleMatch!"]
   [:p "Sign in to continue:"]
   [:div
    [:button
     {:style {:margin-right "1rem"}
      :on-click #(rf/dispatch [:sign-in-with-google])}
     "Sign in with Google"]
    [:button
     {:on-click #(rf/dispatch [:sign-in-with-facebook])}
     "Sign in with Facebook"]]])

(defn home-panel []
  (let [user @(rf/subscribe [:user])]
    [:div {:style {:margin "2rem"}}
     [:h1 "You are logged in!"]
     [:p (str "UID: " (:uid user))]
     [:p (str "Name: " (:display-name user))]
     [:p (str "Email: " (:email user))]
     (when (:photo-url user)
       [:img {:src (:photo-url user)
              :alt "Profile"
              :style {:max-width "100px"
                      :border-radius "50%"}}])]))

(defn main-panel []
  (let [user @(rf/subscribe [:user])]
    (if user
      [home-panel]
      [login-panel])))

;; -------------------------------------------------------
;; 4) Initialization
;; -------------------------------------------------------

(defn ^:dev/after-load mount-root []
  (rdom/render [main-panel]
               (.getElementById js/document "app")))

(defn init []
  (rf/dispatch-sync [:initialize])
  (mount-root))