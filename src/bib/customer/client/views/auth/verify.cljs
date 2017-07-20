(ns bib.customer.client.core.views.auth.verify
  (:require
   [reagent.core :as reagent :refer [atom]]
   [bouncer.core :as b]
   [bouncer.validators :as v]
   [bib.customer.client.views.utils :as view-utils]
   [bib.customer.client.events.auth :as auth-events]
   [bib.customer.client.state.auth :as auth-state]))

(def subscribe (partial auth-state/subscribe-form :verify))

(defn validate-view [form-vals]
  (b/validate form-vals
              :email [v/required v/email]
              :code v/required))

(defn view [state]
  (let [email-cursor (subscribe :email)
        code-cursor (subscribe :code)
        flash-cursor (subscribe :flash)
        touched (atom {:email false :code false})
        has-verr (fn [vs field]
                   (and
                    (contains? vs field)
                    (field @touched)))
        fields (subscribe)]
    (fn []
      (let [verrs (first (validate-view @fields))]
        [:div.row>div.col-sm-6.col-sm-offset-3.col-md-6.col-md-offset-3.col-lg-4.col-lg-offset-4
         [:div.user-verify.auth-panel
          [:div.panel.panel-default
           [:div.panel-heading>h1 "Confirm Your Account"]
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
                :on-key-press
                (view-utils/on-enter-fn auth-events/verify-user)
                :on-change
                (comp
                 (fn [_] (swap! touched assoc :email true))
                 (view-utils/swap-on-value-changed! email-cursor))}]
              (when (has-verr verrs :email)
                [:small.text-help (:email verrs)])]
             [:div.form-group
              {:class (when (has-verr verrs :code) "has-error")}
              [:label {:for "code-input"} "Code"]
              [:input#code-input.form-control
               {:type "code" :placeholder "Code"
                :value @code-cursor
                :on-key-press
                (view-utils/on-enter-fn auth-events/verify-user)
                :on-change
                (comp
                 (fn [_] (swap! touched assoc :code true))
                 (view-utils/swap-on-value-changed! code-cursor))}]
              (when (has-verr verrs :code)
                [:small.text-help (:code verrs)])]
             [:a.btn.btn-default.btn-block
              {:on-click #(auth-events/verify-user)
               :class (when-not (empty? verrs)
                        "disabled")}
              "Verify"]] ]]
          [:a.btn.btn-secondary.btn-block {:on-click
                               (fn [_]  (when (some? @email-cursor)
                                          (auth-events/resend-verification-code)))}
           "Resend Confirmation Code?"]] ] ))))
