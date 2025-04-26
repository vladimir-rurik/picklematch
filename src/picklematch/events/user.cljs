(ns picklematch.events.user
  (:require
   [re-frame.core :as rf]
   [picklematch.firebase.auth :as fba]
   [picklematch.firebase.init :refer [auth-inst]]
   [picklematch.firebase.firestore :as fbf]))

;; 1) Register with email => create the user => set active=false => sendEmailVerification
(rf/reg-event-fx
 :register-with-email
 (fn [{:keys [db]} [_ email password]]
   {:db db
    :dispatch [:clear-auth-states]
    :firebase/create-email-user [email password]}))

(rf/reg-fx
 :firebase/create-email-user
 (fn [[email password]]
   (fba/create-user-with-email!
    email
    password
    (fn [cred]
      (let [uid   (.-uid (.-user cred))
            email (.-email (.-user cred))]
        ;; store Firestore doc => active=false for new email/password user
        (fbf/store-user-if-new! uid email false)

        ;; send built-in verification email
        ;; In user.cljs, in the :firebase/create-email-user handler
        (fba/send-user-verification!
         (fn []
           (rf/dispatch [:auth-message
                         "Check your inbox to verify your email before proceeding."]))
         (fn [err]
           (rf/dispatch [:auth-error "Failed to send verification email. Please try again."])))


        ;; Optionally log them in right away in the UI
        (rf/dispatch [:login-success {:uid uid :email email}])
        (rf/dispatch [:auth-message
                      "Check your inbox to verify your email before proceeding."])))
    (fn [err]
      (js/console.error "Error creating user with email/password:" err)
      (rf/dispatch [:auth-error (.-code err)])))))

;; 5) On next sign-in or onAuthStateChanged, we check emailVerified
(rf/reg-event-fx
 :check-verification
 (fn [{:keys [db]} _]
   {:db (assoc db :loading? true)
    :firebase/check-verification true}))

(rf/reg-fx
 :firebase/check-verification
 (fn [_]
   (let [user (.-currentUser auth-inst)]
     (if user
       (-> (.reload user)
           (.then (fn []
                    (rf/dispatch [:loading? false])
                    (if (.-emailVerified user)
                      (do
                        (rf/dispatch [:user-is-now-verified])
                        (rf/dispatch [:auth-message "Your email is verified!"]))
                      (rf/dispatch [:auth-error "Your email is not yet verified. Please check your inbox."]))))
           (.catch (fn [error]
                     (rf/dispatch [:loading? false])
                     (rf/dispatch [:auth-error (str "Error checking verification: " (.-message error))]))))
       (do
         (rf/dispatch [:loading? false])
         (rf/dispatch [:auth-error "No user is currently signed in."]))))))


(rf/reg-event-fx
 :user-verified
 (fn [{:keys [db]} [_ uid]]
   {:db db
    :firebase/set-active [uid true]}))

(rf/reg-fx
 :firebase/set-active
 (fn [[uid active?]]
   (fbf/set-user-active! uid active?)))
