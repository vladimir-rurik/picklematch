(ns picklematch.views.main
  (:require
   [re-frame.core :as rf]
   [picklematch.views.login :refer [login-panel, verification-check-panel]]
   [picklematch.views.home :refer [home-panel]]))

(defn main-panel []
  (let [user     @(rf/subscribe [:user])     ;; e.g. {:uid "xyz" :email "someone@example.com" :role "ordinary"}
        players  @(rf/subscribe [:players])  ;; map of uid -> {:active true/false, :rating ..., etc.}
        loading? @(rf/subscribe [:loading?])]
    (cond
      loading?
      [:div "Loading..."]

      (nil? user)
      [login-panel] ;; no user => show login

      :else
      (let [uid       (:uid user)
            user-doc  (get players uid)
            active?   (:active user-doc)]
        (if active?
          [home-panel] ;; fully active => go home
          [:div
           [:h2 "Please Verify Your Email"]
           [:p "We have sent you a verification email.
        Once verified, please click the button below."]
           [verification-check-panel user]
           ])))))
