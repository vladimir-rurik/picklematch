(ns picklematch.views.login
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]))

(defn email-password-login-panel []
  (let [email (r/atom "")
        pass  (r/atom "")
        loading? (r/atom false)]
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
                  :disabled @loading?
                  :on-change #(reset! email (-> % .-target .-value))}]
         [:input {:type "password"
                  :placeholder "Enter password"
                  :value @pass
                  :disabled @loading?
                  :on-change #(reset! pass (-> % .-target .-value))}]

         ;; Request a pending registration
         [:button.btn-primary
          {:on-click (fn []
                       (reset! loading? true)
                       (rf/dispatch [:register-with-email @email @pass])
                       ;; Reset loading after a delay
                       (js/setTimeout #(reset! loading? false) 3000))
           :disabled @loading?}
          (if @loading? "Registering..." "Request Registration")]

         ;; Sign In
         [:button.btn-secondary
          {:on-click (fn []
                       (reset! loading? true)
                       (rf/dispatch [:sign-in-with-email @email @pass])
                       ;; Reset loading after a delay
                       (js/setTimeout #(reset! loading? false) 3000))
           :disabled @loading?}
          (if @loading? "Signing In..." "Sign In")]]))))

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

(defn verification-check-panel []
  [:div.fancy-panel
   [:h4 "Email Verification"]
   [:p "If you've already verified your email but haven't been redirected, click below:"]
   [:button.btn-primary
    {:on-click #(rf/dispatch [:check-verification])}
    "Check Verification Status"]])

(defn login-panel []
  ;; Create component with lifecycle method
  (r/create-class
   {:component-did-mount
    (fn [_]
      ;; Clear any lingering errors/messages when login panel mounts
      (rf/dispatch [:clear-auth-states]))

    :reagent-render
    (fn []
      (let [active-tab (r/atom :google)] ;; Default to Google sign-in
        (fn []
          (let [error @(rf/subscribe [:auth-error])
                msg   @(rf/subscribe [:auth-message])]
            [:div.login
             [:h2 "PickleMatch Login"]

             ;; Display errors/messages if any
             (when error
               [:div.error-message
                {:style {:background "#f8d7da"
                         :color "#721c24"
                         :padding "0.75rem"
                         :marginBottom "1rem"
                         :borderRadius "4px"}}
                error])

             (when msg
               [:div.success-message
                {:style {:background "#d4edda"
                         :color "#155724"
                         :padding "0.75rem"
                         :marginBottom "1rem"
                         :borderRadius "4px"}}
                msg])

             ;; Auth method tabs
             [:div.auth-tabs
              {:style {:display "flex"
                       :marginBottom "1rem"}}
              [:div.tab
               {:style {:padding "0.5rem 1rem"
                        :cursor "pointer"
                        :borderBottom (if (= @active-tab :google)
                                        "2px solid #007bff"
                                        "2px solid transparent")}
                :on-click #(do
                             (reset! active-tab :google)
                             (rf/dispatch [:clear-auth-states]))}
               "Google Sign-In"]
              [:div.tab
               {:style {:padding "0.5rem 1rem"
                        :cursor "pointer"
                        :borderBottom (if (= @active-tab :email-password)
                                        "2px solid #007bff"
                                        "2px solid transparent")}
                :on-click #(do
                             (reset! active-tab :email-password)
                             (rf/dispatch [:clear-auth-states]))}
               "Email/Password"]
              [:div.tab
               {:style {:padding "0.5rem 1rem"
                        :cursor "pointer"
                        :borderBottom (if (= @active-tab :email-link)
                                        "2px solid #007bff"
                                        "2px solid transparent")}
                :on-click #(do
                             (reset! active-tab :email-link)
                             (rf/dispatch [:clear-auth-states]))}
               "Email Link"]]

             ;; Active auth method content
             (case @active-tab
               :google
               [:div.auth-content
                [:button.btn-primary
                 {:style {:width "100%"
                          :marginBottom "1rem"}
                  :on-click #(rf/dispatch [:sign-in-with-google])}
                 "Sign in with Google"]]

               :email-password
               [email-password-login-panel]

               :email-link
               [email-link-login-panel])]))))}))