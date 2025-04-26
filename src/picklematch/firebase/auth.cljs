(ns picklematch.firebase.auth
  (:require
   [picklematch.firebase.init :refer [auth-inst]]
   ["firebase/auth" :refer
    [GoogleAuthProvider FacebookAuthProvider signInWithPopup signOut
     createUserWithEmailAndPassword signInWithEmailAndPassword
     sendSignInLinkToEmail signInWithEmailLink isSignInWithEmailLink]]))


;; Google Sign-In
(defn google-sign-in []
  (let [provider (GoogleAuthProvider.)]
    (-> (signInWithPopup auth-inst provider)
        (.then (fn [result]
                 (js/console.log "Google sign-in success" result)))
        (.catch (fn [err]
                  (js/console.error "Google sign-in error" err))))))

;; Facebook Sign-In
(defn facebook-sign-in []
  (let [provider (FacebookAuthProvider.)]
    (-> (signInWithPopup auth-inst provider)
        (.then (fn [result]
                 (js/console.log "Facebook sign-in success" result)))
        (.catch (fn [err]
                  (js/console.error "Facebook sign-in error" err))))))

;; Logout
(defn logout []
  (-> (signOut auth-inst)
      (.then #(js/console.log "Successfully signed out."))
      (.catch #(js/console.error "Error signing out:" %))))

;; Email & Password
(defn create-user-with-email! [email password on-success on-fail]
  (-> (createUserWithEmailAndPassword auth-inst email password)
      (.then on-success)
      (.catch on-fail)))

(defn sign-in-with-email! [email password on-success on-fail]
  (-> (signInWithEmailAndPassword auth-inst email password)
      (.then on-success)
      (.catch on-fail)))

;; Email Link Auth
(defonce action-code-settings
  (clj->js {:url "http://localhost:3000/finishSignIn"
            :handleCodeInApp true}))

(defn send-email-link! [email]
  (-> (sendSignInLinkToEmail auth-inst email action-code-settings)
      (.then (fn []
               (js/localStorage.setItem "emailForSignIn" email)
               (js/console.log "Email link sent to" email)))
      (.catch #(js/console.error "Error sending email link" %))))

(defn is-email-link-sign-in? [url]
  (isSignInWithEmailLink auth-inst url))

(defn complete-email-link-sign-in! [url]
  (let [email (.getItem js/localStorage "emailForSignIn")]
    (when (isSignInWithEmailLink auth-inst url)
      (-> (signInWithEmailLink auth-inst email url)
          (.then (fn [result]
                   (js/localStorage.removeItem "emailForSignIn")
                   (js/console.log "Email link sign-in success!" result)))
          (.catch #(js/console.error "Error completing email link sign-in" %))))))

;; Send built-in verification email
;; Updated send-user-verification! function for auth.cljs
(defn send-user-verification! [on-success on-fail]
  (let [user (.-currentUser auth-inst)]
    (if user
      (-> (.sendEmailVerification user)
          (.then (fn []
                   (js/console.log "Verification email sent successfully.")
                   (when on-success (on-success))))
          (.catch (fn [error]
                    (js/console.error "Error sending verification email:" error)
                    (when on-fail (on-fail error)))))
      (do
        (js/console.error "No current user to send verification to")
        (when on-fail (on-fail "No current user"))))))

