(ns bib.customer.client.core.views.auth.signin
  (:require
   [reagent.core :as reagent :refer [atom]]
   [bouncer.core :as b]
   [bouncer.validators :as v]
   [bib.customer.client.store :refer [app-state nav!]]
   [bib.customer.client.views.utils :as view-utils]
   [bib.customer.client.events.auth :as auth-events]
   [bib.customer.client.state.auth :as auth-state]))

(def subscribe (partial auth-state/subscribe-form :signin))

(defn validate-view [form-vals]
  (b/validate form-vals
              :email [v/required v/email]
              :password v/required))

(defn view [state]
  (let [email-cursor (subscribe :email)
        password-cursor (subscribe :password)
        flash-cursor (subscribe :flash)
        touched (atom {:email false :password false :confirm-password false})
        has-verr (fn [vs field]
                   (and
                    (contains? vs field)
                    (field @touched)))
        fields (subscribe)]
    (fn []
      (let [verrs (first (validate-view @fields))
            signed-in (not-empty (:cognito-user @app-state))
            t (get-in @app-state [:cognito-user :jwt-token])]
        (if signed-in
          (do (nav! :home)
              [:div])
          (do [:div.row
           [:div.col-sm-6.col-sm-offset-3.col-md-6.col-md-offset-3.col-lg-4.col-lg-offset-4
            [:div.user-signin.auth-panel
             [:div.panel.panel-default
              [:div.panel-heading>h1 "Welcome back"]
              [:div.panel-body
               (into [:div]
                     (map
                      (fn [v] [:div.alert
                               {:class (name (:type v))}
                               (:message v)])
                      @flash-cursor))
               [:div.form
                [:div.form-group
                 {:class (when (has-verr verrs :email) "has-error")}
                 [:label {:for "email-input"} "Email Address"]
                 [:input#email-input.form-control
                  {:type "email" :placeholder "Email"
                   :value @email-cursor
                   :on-change
                   (comp
                    (fn [_] (swap! touched assoc :email true))
                    (view-utils/swap-on-value-changed! email-cursor))
                   :on-key-press
                   (view-utils/on-enter-fn auth-events/signin-user)
                   }]
                 (when (has-verr verrs :email)
                   [:small.help-block (:email verrs)])]
                [:div.form-group.password-form-group
                 {:class (when (has-verr verrs :password) "has-error")}
                 [:label {:for "password-input"} "Password"]
                 [:input#password-input.form-control
                  {:type "password" :placeholder "Password"
                   :value @password-cursor
                   :on-change
                   (comp
                    (fn [_] (swap! touched assoc :password true))
                    (view-utils/swap-on-value-changed! password-cursor) )
                   :on-key-press
                   (view-utils/on-enter-fn auth-events/signin-user)
                   }]
                 (when (has-verr verrs :password)
                   [:small.help-block (:password verrs)])]
                [:div.form-group.clearfix
                 [:small [:a.pull-right {:href "#/forgot-password"} "Forgot your password?"] ]]
                [:a.btn.btn-default.btn-block
                 {:on-click #(auth-events/signin-user)
                  :class (when-not (empty? verrs)
                           "disabled") }
                 "Sign in to your account"]]]]
             [:a.btn.btn-secondary.btn-block {:href "#/register"}
              [:span "Don't have an account? " ]
              [:u "Sign up" ]] ] ] ] ) )))))
