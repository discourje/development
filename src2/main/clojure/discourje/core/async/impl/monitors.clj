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

(defn- runtime-exception [lts current-states type message sender receiver]
  (ex-info (str "[SESSION FAILURE] Action "
                (case type :sync "‽" :send "!" :receive "?" :close "C" (throw (Exception.)))
                "("
                (if (nil? message) "" (str message ","))
                sender
                ","
                receiver
                ") is not enabled in current state(s): "
                current-states
                ". LTS in Aldebaran format:\n\n"
                lts
                "\n\n")
           {;:lts            lts
            ;:current-states current-states
            :type           type
            :message        message
            :sender         sender
            :receiver       receiver}))

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

        (if (compare-and-set! (.-flag monitor) false true)
          (if (compare-and-set! (.-current_states monitor) source-states target-states)
            (if (empty? target-states)
              (runtime-exception (.-lts monitor) source-states type message sender receiver)
              true)
            (do
              (reset! (.-flag monitor) false)
              (recur)))
          (recur))))))

(defn lower-flag!
  [monitor]
  {:pre [(or (monitor? monitor) (nil? monitor))]}
  (if (nil? monitor)
    nil
    (do (reset! (.-flag monitor) false)
        nil)))