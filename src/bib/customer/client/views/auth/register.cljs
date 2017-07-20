(ns bib.customer.client.core.views.auth.register
  (:require
   [reagent.core :as reagent :refer [atom]]
   [bouncer.core :as b]
   [bouncer.validators :as v]
   [bib.customer.client.views.utils :as view-utils]
   [bib.customer.client.events.auth :as auth-events]
   [bib.customer.client.state.auth :as auth-state]))

(def subscribe (partial auth-state/subscribe-form :register))

(defn validate-view [form-vals]
  (b/validate form-vals
              :email [v/required v/email]
              :password v/required
              :confirm-password [v/required [(fn [cp]
                                               (= cp (:password form-vals)))
                                             :message
                                             "Password and Confirm Password must match"]]))

(defn view []
  (let [email-cursor (subscribe :email)
        password-cursor (subscribe :password)
        confirm-password-cursor (subscribe :confirm-password)
        flash-cursor (subscribe :flash)
        touched (atom {:email false :password false :confirm-password false})
        has-verr (fn [vs field]
                   (and
                    (contains? vs field)
                    (field @touched)))
        fields (subscribe)]
    (fn []
      (let [verrs (first (validate-view @fields))]
        [:div.row>div.col-sm-6.col-sm-offset-3.col-md-6.col-md-offset-3.col-lg-4.col-lg-offset-4
         [:div.user-register.auth-panel
          [:div.panel.panel-default
           [:div.panel-heading>h1 "Create Your Account"]

           [:div.panel-body
            (into [:div]
                  (map
                   (fn [v] [:div.alert.alert-danger
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
                :on-key-press
                (view-utils/on-enter-fn auth-events/register-new-user)
                :on-change
                (comp
                 (fn [_] (swap! touched assoc :email true))
                 (view-utils/swap-on-value-changed! email-cursor))}]
              (when (has-verr verrs :email)
                [:small.text-help (:email verrs)])]
             [:div.form-group
              {:class (when (has-verr verrs :password) "has-error")}
              [:label {:for "password-input"} "Password"]
              [:input#password-input.form-control
               {:type "password" :placeholder "Password"
                :value @password-cursor
                :on-key-press
                (view-utils/on-enter-fn auth-events/register-new-user)
                :on-change
                (comp
                 (fn [_] (swap! touched assoc :password true))
                 (view-utils/swap-on-value-changed! password-cursor) )}]
              (when (has-verr verrs :password)
                [:small.text-help (:password verrs)])]
             [:div.form-group
              {:class (when (has-verr verrs :confirm-password) "has-error")}
              [:label {:for "confirm-password-input"} "Confirm Password"]
              [:input#password-input.form-control
               {:type "password" :placeholder "Confirm Password"
                :value @confirm-password-cursor
                :on-key-press
                (view-utils/on-enter-fn auth-events/register-new-user)                
                :on-change
                (comp
                 (fn [_] (swap! touched assoc :confirm-password true))
                 (view-utils/swap-on-value-changed! confirm-password-cursor))}]
              (when (has-verr verrs :confirm-password)
                [:small.text-help (:confirm-password verrs)])]
             [:a.btn.btn-default.btn-block
              {:on-click #(auth-events/register-new-user)
               :class (when-not (empty? verrs)
                        "disabled")}
              "Register"]] ]
           ]
[:a.btn.btn-secondary.btn-block {:href "#/signin"}
            [:span "Already have an account? " ]
            [:u "Sign in" ]]

          ] ] ))))
