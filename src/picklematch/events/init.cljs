(ns picklematch.events.init
  (:require
   [re-frame.core :as rf]))

(rf/reg-event-fx ; Changed from reg-event-db to reg-event-fx
 :initialize
 (fn [_ _]
   (js/console.log "Initializing app-db")
   {:db {:user nil ; Initial db state
         :players {}
         :games []
         :selected-date (js/Date.)
         :all-game-dates #{}
         :log-of-rating-events []
         :loading? false
         :auth-error nil
         :auth-message nil}
    :dispatch [:initialize-default-locations] ; Dispatch location initialization
    :dispatch-later [{:ms 100 :dispatch [:load-all-locations]}]})) ; Dispatch location loading slightly later

(rf/reg-event-db
 :auth-error
 (fn [db [_ code]]
   (let [msg (case code
               "auth/email-already-in-use"          "That email is already registered."
               "auth/weak-password"                 "Password must be at least 6 characters."
               "auth/invalid-login-credentials"     "Invalid email/password. Please try again."
               "auth/user-not-found"                "No account found for that email."
               "auth/missing-password"              "You must enter a password."
               "auth/quota-exceeded"                "Email limit exceeded. Please try again later."
               "invalid-confirmation-token"         "Invalid or expired confirmation token."
               "Something went wrong.")]
     (assoc db :auth-error msg))))

(rf/reg-event-db
 :clear-auth-error
 (fn [db _]
   (assoc db :auth-error nil)))

(rf/reg-event-db
 :clear-auth-message
 (fn [db _]
   (assoc db :auth-message nil)))

(rf/reg-event-db
 :auth-message
 (fn [db [_ msg]]
   (assoc db :auth-message msg)))

(rf/reg-event-db
 :clear-auth-states
 (fn [db _]
   (-> db
       (assoc :auth-error nil)
       (assoc :auth-message nil))))
