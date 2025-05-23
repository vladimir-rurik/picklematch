(ns picklematch.events.auth
  (:require
   [re-frame.core :as rf]
   [picklematch.firebase.auth :as fba]
   [picklematch.firebase.firestore :as fbf]))

;; Sign in with Google
(rf/reg-event-fx
 :sign-in-with-google
 (fn [{:keys [db]} _]
   {:db db
    :dispatch [:clear-auth-states]
    :firebase/google-sign-in true}))

(rf/reg-fx
 :firebase/google-sign-in
 (fn [_]
   (fba/google-sign-in)))

;; Sign in with Facebook
(rf/reg-event-fx
 :sign-in-with-facebook
 (fn [{:keys [db]} _]
   {:db db
    :dispatch [:clear-auth-states]
    :firebase/facebook-sign-in true}))

(rf/reg-fx
 :firebase/facebook-sign-in
 (fn [_]
   (fba/facebook-sign-in)))

;; Sign in existing user with email/password
(rf/reg-event-fx
 :sign-in-with-email
 (fn [{:keys [db]} [_ email password]]
   {:db db
    :dispatch [:clear-auth-states]
    :firebase/sign-in-email [email password]}))

(rf/reg-fx
 :firebase/sign-in-email
 (fn [[email password]]
   (fba/sign-in-with-email!
    email password
    (fn [cred]
      (rf/dispatch
       [:login-success
        {:uid   (.-uid (.-user cred))
         :email (.-email (.-user cred))
         :auth-method "email-password"}]))
    (fn [err]
      (rf/dispatch [:auth-error (.-code err)])))))

;; Once sign-in is successful, store user in DB & Firestore if new
(rf/reg-event-fx
 :login-success
 (fn [{:keys [db]} [_ {:keys [uid email auth-method]}]]
   (when (and auth-method (not= auth-method "email-password"))
     ;; Only create new user entries with active=true for non-email-password methods
     ;; like Google or email link (which don't require verification)
     (fbf/store-user-if-new! uid email true))
   
   {:db (assoc db :user {:uid uid :email email}
               :auth-error nil    ;; Clear any errors
               :auth-message nil) ;; Clear any messages
    :dispatch-n [[:load-user-details uid]
                 [:load-all-users]
                 [:load-all-game-dates]
                 [:load-games-for-date (-> (js/Date.) .toISOString (subs 0 10))]]}))

(rf/reg-event-fx
 :load-user-details
 (fn [{:keys [db]} [_ uid]]
   {:db (assoc db :loading? true)
    :firebase/load-user-details [uid true]}))

(rf/reg-fx
 :firebase/load-user-details
 (fn [[uid _dispatch?]]
   (fbf/load-user-doc!
    uid
    (fn [user-data]
      (rf/dispatch [:user-details-loaded user-data]))
    (fn [err]
      (js/console.error "Error loading user doc:" err)
      (rf/dispatch [:user-details-loaded nil])))))

(rf/reg-event-db
 :user-details-loaded
 (fn [db [_ user-data]]
   (if user-data
     (let [uid (:uid user-data)]
       (-> db
           (assoc :loading? false)
           (assoc-in [:players uid] (select-keys user-data [:rating :role :email :active]))
           (assoc-in [:user :role] (:role user-data))))
     (assoc db :loading? false))))

;; Toggle admin role
(rf/reg-event-fx
 :toggle-admin-role
 (fn [{:keys [db]} _]
   (let [uid          (get-in db [:user :uid])
         current-role (get-in db [:user :role] "ordinary")
         new-role     (if (= current-role "admin") "ordinary" "admin")]
     {:db (assoc-in db [:user :role] new-role)
      :firebase/update-user-role [uid new-role]})))

(rf/reg-fx
 :firebase/update-user-role
 (fn [[uid new-role]]
   (fbf/update-user-role! uid new-role)))

;; Logout
(rf/reg-event-fx
 :logout
 (fn [{:keys [db]} _]
   {:db (assoc db :user nil)
    :firebase/logout true
    :dispatch [:clear-auth-states]}))

(rf/reg-fx
 :firebase/logout
 (fn [_]
   (fba/logout)))

;; Email link sign in
(rf/reg-event-fx
 :send-email-link
 (fn [{:keys [db]} [_ email]]
   {:db db
    :dispatch [:clear-auth-states]
    :firebase/send-email-link email}))

(rf/reg-fx
 :firebase/send-email-link
 (fn [email]
   (fba/send-email-link! email
                         ;; Add success callback to show UI message
                         #(rf/dispatch [:auth-message (str "Email link sent to " email)])
                         ;; Add error callback to show error in UI
                         #(rf/dispatch [:auth-error (.-code %)]))))

(rf/reg-event-fx
 :verified-sign-in
 (fn [{:keys [db]} [_ email password]]
   {:db db
    :dispatch [:clear-auth-states]
    :firebase/verified-sign-in [email password]}))

(rf/reg-fx
 :firebase/verified-sign-in
 (fn [[email password]]
   (fba/sign-in-with-email!
    email password
    (fn [cred]
      (let [user (.-user cred)
            uid (.-uid user)]
        ;; First check if email is verified
        (if (.-emailVerified user)
          (do
            ;; Email is verified, update active status and login
            (rf/dispatch [:user-is-now-verified])
            (rf/dispatch
             [:login-success
              {:uid uid
               :email email
               :auth-method "email-password"}])
            (rf/dispatch [:auth-message "Login successful! Email verified."]))
          ;; Email is not verified
          (rf/dispatch [:auth-error "Your email is still not verified. Please check your inbox."]))))
    (fn [err]
      (rf/dispatch [:auth-error (.-code err)])))))

(rf/reg-event-fx
 :check-email-link
 (fn [{:keys [db]} _]
   {:db db
    :firebase/complete-email-link-sign-in true}))

(rf/reg-fx
 :firebase/complete-email-link-sign-in
 (fn [_]
   (let [current-url (.-href js/window.location)]
     (if (fba/is-email-link-sign-in? current-url)
       (fba/complete-email-link-sign-in!
        current-url
        ;; Add success and failure callbacks
        (fn [result]
          (let [user (.-user result)
                uid (.-uid user)
                email (.-email user)]
            (rf/dispatch [:login-success {:uid uid :email email :auth-method "email-link"}])
            (rf/dispatch [:auth-message "Successfully signed in via email link!"])))
        (fn [error]
          (rf/dispatch [:auth-error (.-code error)])))
       (js/console.log "Not a sign-in with email link")))))
