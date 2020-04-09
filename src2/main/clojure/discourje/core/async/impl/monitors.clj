(ns discourje.core.async.impl.monitors
  (:require [discourje.core.async.impl.lts :as lts]))

(deftype Monitor [current-states]
  Object
  (toString [_] (str @current-states)))

(defn monitor? [x]
  (= (type x) Monitor))

(defn monitor [lts]
  {:pre [(lts/lts? lts)]}
  (->Monitor (atom (lts/initial-states lts))))

(defn verify-send! [message sender receiver monitor]
  (try
    (do
      (swap! (.-current_states monitor)
             (fn [source-states]
               (let [target-states (lts/expand-and-send! source-states message sender receiver)]
                 (if (empty? target-states)
                   (throw (Exception.))
                   target-states))))
      true)
    (catch Exception _ false)))

(defn verify-receive! [sender receiver monitor]
  (try
    (do
      (swap! (.-current_states monitor)
             (fn [source-states]
               (let [target-states (lts/expand-and-receive! source-states sender receiver)]
                 (if (empty? target-states)
                   (throw (Exception.))
                   target-states))))
      true)
    (catch Exception _ false)))

(defn verify-close! [sender receiver monitor]
  (try
    (do
      (swap! (.-current_states monitor)
             (fn [source-states]
               (let [target-states (lts/expand-and-close! source-states sender receiver)]
                 (if (empty? target-states)
                   (throw (Exception.))
                   target-states))))
      true)
    (catch Exception _ false)))