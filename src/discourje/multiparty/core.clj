(ns discourje.multiparty.core
  (:require [clojure.core.async :as async :refer :all]
            [clojure.core :refer :all])
  (:import (clojure.lang Seqable)))

;Defines a communication channel with a sender, receiver (strings) and a channel Async.Chan.
(defrecord communicationChannel [sender receiver channel])

;Define a monitor to check communication, this will be used to verify correct conversation.
;This is just a data structure to group related information.
(defrecord monitor [action from to])
;We also need a data structure to create a conditional with branches.
;When the protocol encounters this it will check the conditional and continue on the correct branch.
(defrecord choice [trueMonitor falseMonitor trueBranch falseBranch])

(defn- generateChannel
  "function to generate a channel between sender and receiver"
  [sender receiver]
  (->communicationChannel sender receiver (chan)))

(defn- uniqueCartesianProduct
  "Generate channels between all participants and filter out duplicates e.g.: buyer1<->buyer1"
  [x y]
  (filter some?
          (for [x x y y]
            (when (not (identical? x y))
              (vector x y)))))

(defn generateChannels
  "Generates communication channels between all participants"
  [participants]
  (map #(apply generateChannel %) (uniqueCartesianProduct participants participants)))

(defn putMessage
  "Puts message on the channel, non-blocking"
  [channel message]
  (println (format "setting message %s" message))
  (println channel)
  (go (>! channel message)))

(defn blockingTakeMessage
  "Takes message from the channel, blocking"
  [channel]
  (<!! channel))

(defn getChannel
  "finds a channel based on sender and receiver"
  [sender receiver channels]
  (first
    (filter (fn [ch]
              (and
                (= (:sender ch) sender)
                (= (:receiver ch) receiver)))
            channels)))

(defn- allowSend
  "send is allowed to put on the channel of the active monitor"
  [channel value]
  (if (instance? Seqable channel)
    (for [receiver channel] (putMessage receiver value))
    (putMessage channel value)))

(defn send!
  "send something through the protocol"
  [action value from to protocol]
  (if (nil? (:activeMonitor @protocol))
    (discourje.multiparty.monitor/incorrectCommunication "protocol does not have a defined channel to monitor! Make sure you supply send! with an instantiated protocol!")
    (if (discourje.multiparty.monitor/isCommunicationValid? action from to protocol)
      (let [currentMonitor (:activeMonitor @protocol)]
        (println "oh yes")
        (discourje.multiparty.monitor/activateNextMonitor protocol)
        (allowSend (:channel (getChannel (:from currentMonitor) (:to currentMonitor) (:channels @protocol))) value))
      (discourje.multiparty.monitor/incorrectCommunication (format "Send action: %s is not allowed to proceed from %s to %s" action from to)))))

(defn recv!
  "receive something through the protocol"
  [action from to protocol]
  (discourje.multiparty.TwoBuyersProtocol/communicate action from to))
