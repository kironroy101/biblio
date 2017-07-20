(ns bib.customer.client.views.auth.forgot
  (:require
   [reagent.core :as reagent :refer [atom]]
   [bouncer.core :as b]
   [bouncer.validators :as v]
   [bib.customer.client.views.utils :as view-utils]
   [bib.customer.client.events.auth :as auth-events]
   [bib.customer.client.state.auth :as auth-state]))

(def subscribe (partial auth-state/subscribe-form :forgot))

(defn trigger-recovery-view [state]
  (let [email-cursor (subscribe :email)
        flash-cursor (subscribe :flash)
        fields (subscribe)
        touched (atom {:email false})
        has-verr (fn [vs field]
                   (and
                    (contains? vs field)
                    (field @touched)))]
    (fn []
      [:div.row
       [:div.col-sm-6.col-sm-offset-3.col-md-6.col-md-offset-3.col-lg-4.col-lg-offset-4
        [:div.user-forgot.auth-panel
         [:div.panel.panel-default
          [:div.panel-heading>h1 "Reset Your Password"]
          [:div.panel-body
           (into [:div]
                 (map
                  (fn [v] [:div.alert
                           {:class (name (:type v))}
                           (:message v)])
                  @flash-cursor))
           (let [verrs (first (b/validate @fields
                                          :email [v/required v/email]) )]
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
                  (view-utils/on-enter-fn auth-events/trigger-password-recovery)}]
               (when (has-verr verrs :email)
                 [:small.text-help (:email verrs)])]
              [:a.btn.btn-default.btn-block
               {:on-click #(auth-events/trigger-password-recovery)
                :class (when-not (empty? verrs)
                         "disabled")}
               "Send Password Reset Email"]
              ])]]]]])))

(defn validate-password-reset [form-vals]
  (b/validate form-vals
              :email [v/required v/email]
              :code [v/required]
              :new-password v/required
              :new-password-confirmation [v/required [(fn [cp]
                                                        (= cp (:new-password form-vals)))
                                                      :message
                                                      "Password and Confirm Password must match"]]))

(defn input-recovery-view [state]
  (let [form-fields (subscribe)
        code-cursor (subscribe :code)
        email-cursor (subscribe :email)
        new-password-cursor (subscribe :new-password)
        new-password-confirmation-cursor (subscribe :new-password-confirmation)
        flash-cursor (auth-state/subscribe-form :reset-password :flash)
        fields (subscribe)
        touched (atom {:email false
                       :code false
                       :new-password false
                       :new-password-confirmation false})
        has-verr (fn [vs field]
                   (and
                    (contains? vs field)
                    (field @touched)))]
    (fn []
      (let [verrs (first (validate-password-reset @fields))]
        [:div.row>div.col-sm-6.col-sm-offset-3.col-md-6.col-md-offset-3.col-lg-4.col-lg-offset-4
         [:div.user-forgot.auth-panel
          [:div.panel.panel-default
           [:div.panel-heading>h1 "Reset Your Password"]
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
                (view-utils/on-enter-fn auth-events/confirm-password-recovery)
                :on-change
                (comp
                 (fn [_] (swap! touched assoc :email true))
                 (view-utils/swap-on-value-changed! email-cursor))}]
              (when (has-verr verrs :email)
                [:small.text-help (:email verrs)])]
             [:div.form-group
              {:class (when (has-verr verrs :code) "has-error")}
              [:label {:for "code-input"} "Verification Code"]
              [:input#email-input.form-control
               {:type "text" :placeholder "Code"
                :on-key-press
                (view-utils/on-enter-fn auth-events/confirm-password-recovery)
               :value @code-cursor
                :on-change
                (comp (fn [_] (swap! touched assoc :code true))
                      (view-utils/swap-on-value-changed! code-cursor) )}]
              (when (has-verr verrs :code)
                [:small.text-help (:code verrs)])]
             [:div.form-group
              {:class (when (has-verr verrs :new-password) "has-error")}
              [:label {:for "password-input"} "Password"]
              [:input#password-input.form-control
               {:type "password" :placeholder "Password"
                :value @new-password-cursor
                :on-key-press
                (view-utils/on-enter-fn auth-events/confirm-password-recovery)
                :on-change
                (comp (fn [_] (swap! touched assoc :new-password true))
                      (view-utils/swap-on-value-changed! new-password-cursor))}]
              (when (has-verr verrs :new-password)
                [:small.text-help (:new-password verrs)])]
             [:div.form-group
              {:class (when (has-verr verrs :new-password-confirmation) "has-error")}
              [:label {:for "confirm-password-input"} "Confirm Password"]
              [:input#password-input.form-control
               {:type "password" :placeholder "Confirm Password"
                :value @new-password-confirmation-cursor
                :on-key-press
                (view-utils/on-enter-fn auth-events/confirm-password-recovery)
                :on-change
                (comp (fn [_] (swap! touched assoc :new-password-confirmation true))
                      (view-utils/swap-on-value-changed! new-password-confirmation-cursor))}]
              (when (has-verr verrs :new-password-confirmation)
                [:small.text-help (:new-password-confirmation verrs)])]
             [:a.btn.btn-default.btn-block
              {:on-click #(auth-events/confirm-password-recovery)
               :class (when-not (empty? verrs)
                        "disabled")}
              "Reset Password"]
             ] ]] ] ]))))
