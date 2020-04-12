(ns discourje.core.async.impl.monitors
  (:require [discourje.spec.lts :as lts]))

(deftype Monitor [current-states]
  Object
  (toString [_] (str @current-states)))

(defn monitor? [x]
  (= (type x) Monitor))

(defn monitor [lts]
  {:pre [(lts/lts? lts)]}
  (->Monitor (atom (lts/initial-states lts))))

(defn- verify! [monitor f]
  (try (do (swap! (.-current_states monitor)
                  (fn [source-states]
                    (let [target-states (f source-states)]
                      (if (empty? target-states)
                        (throw (Exception.))
                        target-states))))
           true)
       (catch Exception _ false)))

(defn verify-sync! [message sender receiver monitor]
  (verify! monitor (fn [source-states] (lts/expand-and-sync! source-states message sender receiver))))

(defn verify-send! [message sender receiver monitor]
  (verify! monitor (fn [source-states] (lts/expand-and-send! source-states message sender receiver))))

(defn verify-receive! [sender receiver monitor]
  (verify! monitor (fn [source-states] (lts/expand-and-receive! source-states sender receiver))))

(defn verify-close! [sender receiver monitor]
  (verify! monitor (fn [source-states] (lts/expand-and-close! source-states sender receiver))))
