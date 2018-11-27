(ns discourje.multiparty.monitoring
  (:require [discourje.multiparty.core :refer :all])
  (:import (discourje.multiparty.core monitor choice)
           (clojure.lang Seqable)))

(defn setActiveMonitor
  "Set the active monitor based on the protocol"
  [activeMonitor protocol]
  (let [current (first @protocol)]
    (reset! activeMonitor current)
    (reset! protocol (subvec @protocol 1))))

(defn allowSend
  "send is allowed to put on the channel of the active monitor"
  [channel value]
  (if (instance? Seqable channel)
    (for [receiver channel] (putMessage receiver value))
    (putMessage channel value)))

(defn allowReceive
  "receive is allowed to take from the channel of the active monitor"
  [channel]
  (blockingTakeMessage channel))

(defn incorrectCommunication
  "communication incorrect, log a message! (or maybe throw exception)"
  [message]
  (println message))

(defn validCommunication?
  "Checks if communication is valid by comparing input to the active monitor"
  [action from to activeMonitor]
  (cond
    (instance? monitor @activeMonitor)
    (do
      (println "yes is monitor")
      (println (:to @activeMonitor))
      (and
        (= action (:action @activeMonitor))
        (= from (:from @activeMonitor))
        (and (if (instance? Seqable (:to @activeMonitor))
               (contains? (:to @activeMonitor) to)
               (= to (:to @activeMonitor))))))
    (instance? choice @activeMonitor)
    (do
      (println "yes is choice"))))

(defn tryCommunicate
  ([action value from to channels activeMonitor protocol]
   (println "sending")
   (if (nil? @activeMonitor)
     (incorrectCommunication "protocol does not have a defined channel to monitor!")
     (if (validCommunication? action from to activeMonitor)
       (let [currentMonitor @activeMonitor]
         (println "oh yes")
         (setActiveMonitor activeMonitor protocol)
         (allowSend (:channel (getChannel (:from currentMonitor) (:to currentMonitor) channels)) value)
         )
       (incorrectCommunication "protocol does not allow diverging from communication channels!"))))
  ([action from to channels activeMonitor protocol]
   (println "listening")
   (if (nil? @activeMonitor)
     (incorrectCommunication "protocol does not have a defined channel to monitor!")
     (if (validCommunication? action from to activeMonitor)
       (let [value (allowReceive (:channel (getChannel (:from @activeMonitor) (:to @activeMonitor) channels)))]
         (setActiveMonitor activeMonitor protocol)
         value)
       (incorrectCommunication "protocol does not allow diverging from communication channels!")))))
