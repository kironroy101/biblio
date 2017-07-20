(ns bib.customer.client.views.home
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
    [bib.customer.client.store :refer [app-state nav!]]
    [bib.customer.client.views.utils :as view-utils]
    [bib.customer.client.events.auth :as auth-events]
    [cljs.core.async :refer [timeout <!]]
    ))

(defn- not-signed-in-auth-links []
  [:ul
   [:li [:a {:href "#/register"} "Register"]]
   [:li [:a {:href "#/verify"} "Verify Registration"]]
   [:li [:a {:href "#/forgot-password"} "Forgot Password"]]
   [:li [:a {:href "#/reset-forgot-password"} "Reset Password"]]
   [:li [:a {:href "#/signin"} "Sign In"]]])

(defn- signed-in-auth-links []
  [:ul
   [:li [:a {:on-click
             #((auth-events/signout-user)
               (nav! :signin))} "Sign Out"]]
   ])

(defn view []
  (let [signed-in (not-empty (:cognito-user @app-state))
        t (get-in @app-state [:cognito-user :jwt-token])]
    (if signed-in
      [:div
       (when (some? t)
         [:div
          [:div.alert.alert-info.session-status "Valid Session: "
           [:span.session-status-bool (str (:valid-session (:cognito-user @app-state)))]]
          [:label "JWT Auth Token - AWS Cognito "]
          [:pre.auth-token t]])]
      (do (nav! :signin)
          [:div]))))
