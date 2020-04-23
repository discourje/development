(ns discourje.core.async.impl.monitors
  (:require [discourje.spec.lts :as lts]))

(deftype Monitor [lts current-states flag])

(defn monitor
  [lts]
  {:pre [(lts/lts? lts)]}
  (->Monitor lts
             (atom (lts/initial-states lts))
             (atom false)))

(defn monitor?
  [x]
  (= (type x) Monitor))

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
  {:pre [(or (monitor? monitor) (nil? monitor))]}
  (if (nil? monitor)
    true
    (loop []
      (let [source-states @(.-current_states monitor)
            target-states (lts/expand-then-perform! source-states
                                                    type
                                                    message
                                                    sender
                                                    receiver)]
        (if (empty? target-states)
          false
          (if (compare-and-set! (.-flag monitor) false true)
            (if (compare-and-set! (.-current_states monitor) source-states target-states)
              true
              (do
                (reset! (.-flag monitor) false)
                (recur)))
            (recur)))))))

(defn lower-flag!
  [monitor]
  {:pre [(or (monitor? monitor) (nil? monitor))]}
  (if (nil? monitor)
    nil
    (do (reset! (.-flag monitor) false)
        nil)))