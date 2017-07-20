(ns bib.customer.client.views.utils
  (:require [reagent.core :as reagent :refer [atom]]))

(defn swap-on-value-changed! [cursor]
  #(swap! cursor (fn [c] (-> % .-target .-value))))

(defn on-enter-fn [cb]
  (fn [e] (when (= 13 (.-charCode e)) (cb))))
