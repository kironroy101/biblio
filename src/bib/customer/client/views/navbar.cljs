(ns bib.customer.client.views.navbar
  (:require [bib.customer.client.store :refer [app-state nav!]]
            [bib.customer.client.views.utils :as view-utils]
            [bib.customer.client.events.auth :as auth-events]))

(defn- not-signed-in-auth-links []
  [:ul.navbar-right
   #_[:li [:a {:href "#/register"} "Register"]]
   #_[:li [:a {:href "#/verify"} "Verify Registration"]]
   #_[:li [:a {:href "#/forgot-password"} "Forgot Password"]]
   #_[:li [:a {:href "#/reset-forgot-password"} "Reset Password"]]
   [:li [:a {:href "#/signin"}
   [:i.fa.fa-sign-in]
   [:span "Sign In" ]]]])

(defn- signed-in-auth-links []
  [:ul.navbar-right
   [:li [:a {:on-click #((auth-events/signout-user)
                         (nav! :signin))}
                         [:i.fa.fa-sign-out]
                         [:span "Sign Out" ]]]])

(defn view []
  [:nav.navbar
   [:div.container-fluid
    (let [signed-in (not-empty (:cognito-user @app-state))]
     [:div
      [:div.navbar-left
        [:a.brand {:href "/"}
        [:img {:src "img/marque-sm.png"}]]]
      (if-not signed-in
        [not-signed-in-auth-links]
        [signed-in-auth-links])
      (when-let [em (get-in @app-state [:cognito-user :email])]
        [:p.navbar-text.navbar-right "Hello, " em])])]])
