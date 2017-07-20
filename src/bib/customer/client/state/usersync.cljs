(ns bib.customer.client.state.usersync
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [mount.core :refer [defstate]])
  (:require [mount.core :as mount]
            [cljs.core.async :as async :refer [timeout chan <! >! alts! close!]]
            [bib.customer.client.events.auth :as auth-events]))

(def user-sync-pid (atom nil))

(defn start-cognito-user-sync []
  (let [close-ch (chan)
        id (random-uuid)]
    (reset! user-sync-pid id)
    (go-loop []
      (when (= @user-sync-pid id)
        (auth-events/update-user-info)
        (<! (timeout 5000))
        (recur)))))

(defstate user-sync
  :start (do
           (start-cognito-user-sync))
  :stop (reset! user-sync-pid nil))
