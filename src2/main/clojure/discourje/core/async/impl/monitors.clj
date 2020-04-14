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

(defn verify! [monitor type message sender receiver]
  (try (do (swap! (.-current_states monitor)
                  (fn [source-states]
                    (let [target-states (lts/expand-then-perform! source-states
                                                                  type
                                                                  message
                                                                  sender
                                                                  receiver)]
                      (if (empty? target-states)
                        (throw (Exception.))
                        target-states))))
           true)
       (catch Exception _ false)))