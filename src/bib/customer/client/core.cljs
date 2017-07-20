(ns bib.customer.client.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [mount.core :refer [defstate]])
  (:require [reagent.core :as reagent :refer [atom]]
            [mount.core :as mount]
            [cljs.core.async :as async :refer [timeout chan <! >! alts! close!]]
            [secretary.core :as secretary :refer-macros [defroute]]
            [goog.events :as events]
            [goog.history.EventType :as EventType]
            [goog.history.Html5History :as Html5History]
            [bib.customer.client.store :refer [app-state nav! register-routes]]
            [bib.customer.client.core.views.auth.register :as register]
            [bib.customer.client.core.views.auth.signin :as signin]
            [bib.customer.client.core.views.auth.verify :as verify]
            [bib.customer.client.views.home :as home]
            [bib.customer.client.views.navbar :as navbar]
            [bib.customer.client.views.auth.forgot :as forgot]
            [bib.customer.client.events.auth :as auth-events]
            [bib.customer.client.state.auth :as auth-state]
            [bib.customer.client.views.utils :as view-utils]
            [bib.customer.client.state.usersync :refer [user-sync]])


  (:import goog.History
           [goog.history Html5History]
           ))

(enable-console-print!)

(defmulti render-view (fn [v & args] v))

(defn wrap-content [& c]
  [:div.container
   [:div.row
    [:div.col-md-12
     c]]])

(defmethod render-view :default
  [_ s]
  (home/view))

(defmethod render-view :register
  [_ s] [register/view])

(defmethod render-view :verify
  [_ s] [verify/view])

(defmethod render-view :signin
  [_ s] [signin/view])

(defmethod render-view :forgot-password-trigger
  [_ s] [forgot/trigger-recovery-view])

(defmethod render-view :forgot-password-complete
  [_ s] [forgot/input-recovery-view])

(defn page []
  (fn [] [:div
          [navbar/view]
          [:div.container
           (render-view (get @app-state :page) app-state)]]))


(defn mount-components []
  (mount/start #'user-sync)
  (reagent/render-component
       [page] (. js/document (getElementById "app"))))

(mount-components)


(defn on-js-reload []
  (mount/stop #'user-sync)
  (mount/start #'user-sync)
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)
