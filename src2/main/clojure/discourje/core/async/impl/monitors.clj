(ns discourje.core.async.impl.monitors
  (:require [discourje.spec.lts :as lts]))

(deftype Monitor [lts current-states])

(defn monitor?
  [x]
  (= (type x) Monitor))

(defn monitor
  [lts]
  {:pre [(lts/lts? lts)]}
  (->Monitor lts (atom (lts/initial-states lts))))

(defn str-lts
  [monitor]
  {:pre [(monitor? monitor)]}
  (str (.-lts monitor)))

(defn str-current-states
  [monitor]
  {:pre [(monitor? monitor)]}
  (let [s (str @(.-current_states monitor))]
    (subs s 1 (dec (count s)))))

(defn verify!
  [monitor type message sender receiver]
  (if (nil? monitor)
    true
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
         (catch Exception _ false))))