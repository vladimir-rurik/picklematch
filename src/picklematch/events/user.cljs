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
    :dispatch [:clear-auth-error]
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
   (let [user (.-currentUser auth-inst)]
     (when user
       (-> (.reload user)
           (.then (fn []
                    (when (.-emailVerified (.-currentUser auth-inst))
                      (rf/dispatch [:user-is-now-verified]))))
           (.catch #(js/console.error "Error reloading user" %))))
     {})))

(rf/reg-event-fx
 :user-is-now-verified
 (fn [{:keys [db]} _]
   (let [uid   (get-in db [:user :uid])
         email (get-in db [:user :email])]
     ;; Mark Firestore doc as active=true
     (fbf/store-user-if-new! uid email true)
     {:db (assoc-in db [:players uid :active] true)})))

(rf/reg-event-fx
 :user-verified
 (fn [{:keys [db]} [_ uid]]
   {:db db
    :firebase/set-active [uid true]}))

(rf/reg-fx
 :firebase/set-active
 (fn [[uid active?]]
   (fbf/set-user-active! uid active?)))
