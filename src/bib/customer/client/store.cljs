(ns bib.customer.client.store
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as reagent :refer [atom]]
            [secretary.core :as secretary]
            [goog.events :as events]
            [goog.history.EventType :as EventType]
            [goog.history.Html5History :as Html5History]
            [clojure.string :as string]
            [cljs.core.async :refer [timeout <!]]
            )
  (:import goog.History
           [goog.history Html5History]
           ))

(defonce app-state
  (atom
   { ;:page :home
    :cognito {:userPoolId "us-east-XXXXXXXXXX"
              :userPoolClientID "XXXXXXXXXXXXXXXXXXXXXXXXX"
              :region "us-east-1"}
    :cognito-user {}
    :auth-form
    {:register {}
     :signin {}
     :verify {}
     :forgot {}
     }}))

(def routes
  {:home "/"
   :register "/register"
   :signin "/signin"
   :verify "/verify"
   :forgot-password-trigger "/forgot-password"
   :forgot-password-complete "/reset-forgot-password"})

(defn set-page [p]
  (swap! app-state assoc :page p))

;; Register the routes
(defn register-routes []
  (doseq [[k uri] routes]
    (secretary/add-route! uri #(set-page k))))
(register-routes)

(defonce history
  (doto (History.)
    (goog.events/listen  EventType/NAVIGATE #(secretary/dispatch! (.-token %)))
    (.setEnabled true)))

(defn nav! [k]
  (go (<! (timeout 1))
    (.setToken history (get routes k))))
