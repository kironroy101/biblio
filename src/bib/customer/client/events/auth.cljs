(ns bib.customer.client.events.auth
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [clojure.string :as string]
            [cljs.core.async :as async :refer [timeout chan <! >! alts! close!]]
            [bib.customer.client.store :refer [app-state nav!]]
            [bib.customer.client.state.auth :as auth-state]))

(def CognitoUserPool (-> js/AmazonCognitoIdentity .-CognitoUserPool))
(def CognitoUserAttribute (-> js/AmazonCognitoIdentity .-CognitoUserAttribute))
(def CognitoUser (-> js/AmazonCognitoIdentity .-CognitoUser))
(def AuthenticationDetails (-> js/AmazonCognitoIdentity .-AuthenticationDetails))
(def AWSCognito js/AWSCognito)

(defn to-username [email]
  (string/replace email "@" "-at-"))

(defn to-email [username]
  (string/replace username "-at-" "@"))

(defn get-user-pool []
  (new CognitoUserPool
       (clj->js
        {:UserPoolId (get-in @app-state [:cognito :userPoolId])
         :ClientId (get-in @app-state [:cognito :userPoolClientID])})))

(defonce user-pool (get-user-pool))

(defn get-authtoken [cb]
  (let [user (-> user-pool .getCurrentUser)]
    (if (some? user)
      (.getSession user
                   (fn [err session]
                     (cond (some? err)
                           (cb err nil)
                           (not (.isValid session))
                           (cb nil nil)
                           :default
                           (cb nil (-> session .getIdToken .getJwtToken)))))
      (cb nil nil))))

(defn- create-cognito-user [email]
  (new CognitoUser
       (clj->js
        {:Username (to-username email)
         :Pool user-pool})))

(defn update-user-info []
  (let [u (.getCurrentUser user-pool)]
    (when (some? u)
      (go
        (let [field-ch (chan)
              err-ch (chan)
              email (to-email (.getUsername u))]
          (swap! app-state assoc-in [:cognito-user :email] email)
          (get-authtoken
           (fn [err t]
             (go (when (some? t) (>! field-ch t))
                 (when (some? err) (>! err-ch err)))))
          (let [[token ch] (alts! [field-ch err-ch (timeout 10000)])]
            (cond
              (= ch field-ch) (swap! app-state assoc-in [:cognito-user :jwt-token] token)
              (= ch err-ch) (do (println "Error Retrieving User JWT Token" token)
                                (auth-state/reset-cognito-user {}))
              :default (do (println "Timeout retrieving User JWT Token")
                           (auth-state/reset-cognito-user {}))))
          (.getSession u
                       (fn [err t]
                         (go (when (some? t) (>! field-ch (.isValid t)))
                             (when (some? err) (>! err-ch err)))))
          (let [[session ch] (alts! [field-ch err-ch (timeout 10000)])]
            (cond
              (= ch field-ch) (swap! app-state assoc-in [:cognito-user :valid-session]
                                     session)
              (= ch err-ch) (do (println "Error Retrieving User Session" session))
              :default (do (println "Timeout retrieving User Session"))))
          (close! field-ch)
          (close! err-ch))) ) ))

(defn configure-aws-cognito []
  (set! (->  AWSCognito .-config .-region) (get-in @app-state [:cognito :region])))


(def flash-register (partial auth-state/conj-form-flash :register))
(def flash-verify (partial auth-state/conj-form-flash :verify))
(def flash-signin (partial auth-state/conj-form-flash :signin))
(def flash-forgot (partial auth-state/conj-form-flash :forgot))
(def flash-reset-password (partial auth-state/conj-form-flash :reset-password))

(defmulti handle-register-error #(keyword (.-name %)))

(defmethod handle-register-error :UsernameExistsException
  [e]
  (flash-register
   {:type :alert-danger
    :message "The username is unavailable. Please choose a different one."}))


(defmethod handle-register-error :InvalidParameterException
  [e]
  (flash-register
   {:type :alert-danger
    :message (.-message e)}))

(defmethod handle-register-error :InvalidPasswordException
  [e]
  (flash-register
   {:type :alert-danger
    :message (str
              "The password specified is invalid. Please make sure you have at"
              " least of each of uppercase letter, lowercase letter, and special character.")}))

(defmethod handle-register-error :default
  [e]
  (flash-register
   {:type :alert-danger
    :message "Could no complete registraion. Please try again. If this persists, please contact system administrator"})
  (println "Unhandled register error" e))

(defn -handle-register-error [e]
  (auth-state/clear-form-flash :register)
  (handle-register-error e))

(defn register-new-user []
  (let [password (get-in @app-state [:auth-form :register :password])
        email (get-in @app-state [:auth-form :register :email])
        attr-email (new CognitoUserAttribute
                        (clj->js {:Name "email" :Value email}))]
    (auth-state/clear-form-flash :register)
    (.signUp user-pool
             (to-username email)
             password
             (to-array [attr-email]) nil
             (fn [err result]
               (if (some? err)
                 (-handle-register-error err)
                 (do (flash-verify {:type :alert-success
                                    :message "Please check your email for a verification code."})
                     (nav! :verify))
                 )))))

(defn verify-user []
  (let [email (get-in @app-state [:auth-form :verify :email])
        code (get-in @app-state [:auth-form :verify :code])
        u (create-cognito-user email)]
    (auth-state/clear-form-flash :verify)
    (.confirmRegistration u
                          code true
                          (fn [err result]
                            (if (some? err)
                              (do (flash-verify
                                   {:type :alert-danger
                                    :message (.-message err)}))
                              (do (println "Verified")
                                  (flash-signin
                                   {:type :alert-success
                                    :message "Successfully Verified. Please sign in."})
                                  (nav! :signin)))))))

(defmulti handle-signin-error #(keyword (.-name %)))


(defn- handle-invalid-pair []
  (flash-signin  {:type :alert-danger
           :message "Invalid Username/Password Pair"}))

;; handles Incorrect Pairs and attempts exceeded
(defmethod handle-signin-error :NotAuthorizedException
  [e]
  (println "Not Authorized" (.-message e))
  (cond
    (some? (string/index-of (.-message e) "Password attempts exceeded"))
    (flash-signin {:type :alert-danger :message "Password attempts exceeded"})
    :default
    (handle-invalid-pair))
  )

(defmethod handle-signin-error :UserNotFoundException
  [e]
  (handle-invalid-pair))

(defmethod handle-signin-error :UserNotConfirmedException
  [e]
  (flash-signin {:type :alert-danger
          :message "User is not confirmed. Please check your email for a verification code."}))

(defmethod handle-signin-error :default
  [e]
  (println "Unhandled Signin Error" e)
  (flash-signin {:type :alert-danger
          :message "Could not complete login"}))

(defn- clear-signin-form []
  (auth-state/clear-form-flash :signin)
  (auth-state/clear-signin-password))

(defn -handle-signin-error [e]
  (clear-signin-form)
  (handle-signin-error e))


(defn signin-user []
  (let [email (get-in @app-state [:auth-form :signin :email])
        password (get-in @app-state [:auth-form :signin :password])
        user (create-cognito-user email)
        auth-details (new AuthenticationDetails
                          (clj->js
                           {:Username (to-username email)
                            :Password password}))]
    (.authenticateUser user
                       auth-details
                       (clj->js
                        {:onSuccess (fn []
                                      (clear-signin-form)
                                      (update-user-info)
                                      (nav! :home))
                         :onFailure -handle-signin-error}))))

(defn signout-user []
  (-> (get-user-pool)
      .getCurrentUser
      .signOut)
  (auth-state/reset-cognito-user {}))

(defn trigger-password-recovery []
  (let [email (get-in @app-state [:auth-form :forgot :email])
        user (create-cognito-user email)]
    (auth-state/clear-form-flash :forgot)
    (.forgotPassword user
                     (clj->js
                      {:onSuccess (fn []
                                    (flash-reset-password
                                     {:type :alert-success
                                      :message "Please check your Email for a recovery code."})
                                    (nav! :forgot-password-complete))
                       :onFailure (fn [e]
                                    (flash-forgot
                                     {:type :alert-danger
                                      :message (.-message e)}))}))
    ))

(defn confirm-password-recovery []
  (let [email (get-in @app-state [:auth-form :forgot :email])
        code (get-in @app-state [:auth-form :forgot :code])
        new-password (get-in @app-state [:auth-form :forgot :new-password])
        user (create-cognito-user email)]
    (auth-state/clear-form-flash :reset-password)
    (.confirmPassword user
                      code
                      new-password
                      (clj->js
                       {:onSuccess (fn []
                                     (flash-signin
                                      {:type :alert-success
                                       :message
                                       "Your password has been reset"})
                                     (nav! :signin))
                        :onFailure (fn [e]
                                     (flash-reset-password
                                      {:type :alert-danger
                                       :message
                                       (.-message e)}))}))))

(defn resend-verification-code []
  (let [email (get-in @app-state [:auth-form :verify :email])]
    (auth-state/clear-form-flash :verify)
    (.resendConfirmationCode (create-cognito-user email)
                             (fn [err r]
                               (if (some? err)
                                 (flash-verify
                                  {:type :alert-danger
                                   :message
                                   (.-message err)})
                                 (flash-verify
                                  {:type :alert-success
                                   :message
                                   "New confirmation code has been sent. Please check your email."}))
                               ))))
