(ns bib.customer.client.state.auth
  (:require
    [reagent.core :as reagent :refer [atom]]
    [bib.customer.client.store :refer [app-state]]))

(defn subscribe-form
  [& path]
  (reagent/cursor app-state (into [] (concat [:auth-form] path) )))

(defn reset-forms []
  (swap! app-state assoc :auth-form
         {:register {}
          :signin {}
          :verify {}
          :forgot {}}))

(defn reset-cognito-user [u]
  (swap! app-state assoc :cognito-user u))

(defn conj-form-flash [form m]
  (swap! app-state update-in [:auth-form form :flash]
         conj m))

(defn clear-form-flash [form]
  (swap! app-state assoc-in [:auth-form form :flash] []))

(defn clear-signin-password []
  (swap! app-state assoc-in [:auth-form :signin :password] nil))

(defn clear-signin-email []
  (swap! app-state assoc-in [:auth-form :signin :email] nil))

(defn clear-signin-form []
  (clear-signin-password)
  (clear-signin-form))
