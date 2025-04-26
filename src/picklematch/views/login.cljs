(ns picklematch.views.login
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]))

(defn email-password-login-panel []
  (let [email (r/atom "")
        pass  (r/atom "")]
    (fn []
      (let [global-error @(rf/subscribe [:auth-error])
            msg         @(rf/subscribe [:auth-message])]
        [:div.fancy-panel
         (when global-error
           [:div {:style {:background "#f8d7da" :color "#721c24"
                          :padding "0.75rem" :marginBottom "1rem"
                          :borderRadius "4px"}}
            global-error])
         (when msg
           [:div {:style {:background "#d4edda" :color "#155724"
                          :padding "0.75rem" :marginBottom "1rem"
                          :borderRadius "4px"}}
            msg])
         [:h4 "Email/Password"]
         [:input {:type "email"
                  :placeholder "Enter email"
                  :value @email
                  :on-change #(reset! email (-> % .-target .-value))}]
         [:input {:type "password"
                  :placeholder "Enter password"
                  :value @pass
                  :on-change #(reset! pass (-> % .-target .-value))}]

         ;; Request a pending registration
         [:button.btn-primary
          {:on-click #(rf/dispatch [:register-with-email @email @pass])}
          "Request Registration"]

         ;; Sign In
         [:button.btn-secondary
          {:on-click #(rf/dispatch [:sign-in-with-email @email @pass])}
          "Sign In"]]))))

(defn email-link-login-panel []
  (let [email (r/atom "")]
    (fn []
    ;;   (let [global-error @(rf/subscribe [:auth-error])]
        [:div.fancy-panel
        ;;  (when global-error
        ;;    [:div {:style {:background "#f8d7da" :color "#721c24"}} global-error])
         [:h4 "Email Link Sign In"]
         [:input {:type "email"
                  :placeholder "Enter email"
                  :value @email
                  :on-change #(reset! email (-> % .-target .-value))}]
         [:button.btn-primary
          {:on-click #(rf/dispatch [:send-email-link @email])}
          "Send Sign-In Link"]])))

;; (defn confirm-pending-user-panel []
;;   (let [dispatched? (r/atom false)]
;;     (fn []
;;       (when-not @dispatched?
;;         (reset! dispatched? true)
;;         (let [search-params (-> js/window.location .-search (js/URLSearchParams.))
;;               token (.get search-params "token")]
;;           (rf/dispatch [:confirm-pending-registration token])))

;;       (let [error @(rf/subscribe [:auth-error])
;;             user  @(rf/subscribe [:user])]
;;         [:div.fancy-panel
;;          (cond
;;            error
;;            [:div "Error confirming user: " error]

;;            user
;;            [:div "Your account has been successfully created!"]

;;            :else
;;            [:div "Confirming, please wait..."])]))))


(defn login-panel []
  [:div.login
   [:h2 "PickleMatch Login"]
   [:button.btn-primary
    {:on-click #(rf/dispatch [:sign-in-with-google])}
    "Sign in with Google"]
;;    [:button.btn-primary
;;     {:on-click #(rf/dispatch [:sign-in-with-facebook])}
;;     "Sign in with Facebook"]
   [email-password-login-panel]
   [email-link-login-panel]])