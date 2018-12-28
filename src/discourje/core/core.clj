(ns discourje.core.core
  (:require [clojure.core.async :as async :refer :all]
            [clojure.core :refer :all])
  (use [discourje.core.monitor :only [incorrectCommunication isReceiveMActive? closeProtocol! activateMonitorOnSend activateMonitorOnReceive isCommunicationValid? activateNextMonitor hasMultipleReceivers? removeReceiver getTargetBranch]])
  (:import (discourje.core.monitor choice sendM)
           (clojure.lang PersistentQueue)))

;Defines a communication channel with a sender, receiver (strings), a channel Async.Chan and a queue for receivers.
;We need a receivers queue to allow for multiple send! operations on the same channel without taking values from it by registered receives
;We could also allow for blocking recv! now, without callbacks, but this needs testing!
(defrecord communicationChannel [sender receiver channel receivingQueue])

(defn- generateChannel
  "function to generate a channel between sender and receiver"
  [sender receiver]
  (->communicationChannel sender receiver (chan) (atom PersistentQueue/EMPTY)))

(defn uniqueCartesianProduct
  "Generate channels between all participants and filters out duplicates e.g.: buyer1<->buyer1"
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
  (put! channel message))

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
  (if (vector? channel)
    (for [receiver channel] (putMessage receiver value))
    (putMessage channel value)))


(defn send!
  "send something through the protocol"
  ([action value from to protocol]
   (if (nil? (:activeMonitor @protocol))
     (incorrectCommunication "protocol does not have a defined channel to monitor! Make sure you supply send! with an instantiated protocol!")
     (if (isCommunicationValid? action from to protocol)
       (let [currentMonitor @(:activeMonitor @protocol)]
         (cond
           (instance? sendM currentMonitor)
           (do (send! currentMonitor value protocol)
             (activateMonitorOnSend action from to protocol))
           (instance? choice currentMonitor)
           (let [target (getTargetBranch action from to protocol)]
             (instance? sendM target)
             (do (send! target value protocol)
               (activateMonitorOnSend action from to protocol))
             )))
       (incorrectCommunication (format "Send action: %s is not allowed to proceed from %s to %s" action from to)))))
  ([currentMonitor value protocol]
   (if (vector? (:to currentMonitor))
     (doseq [receiver (:to currentMonitor)]
       (allowSend (:channel (getChannel (:from currentMonitor) receiver (:channels @protocol))) value))
     (allowSend (:channel (getChannel (:from currentMonitor) (:to currentMonitor) (:channels @protocol))) value))))

(defn recvDelayed!
  "receive something through the protocol"
  [action from to protocol callback]
  (let [channel (getChannel from to (:channels @protocol))
        f (fn [] (take! (:channel channel)
                        (fn [x]
                          (if (nil? (:activeMonitor @protocol))
                            (incorrectCommunication "protocol does not have a defined channel to monitor! Make sure you supply send! with an instantiated protocol!")
                            (if (isCommunicationValid? action from to protocol)
                              (if (hasMultipleReceivers? protocol)
                                (do
                                  (removeReceiver protocol to)
                                  (add-watch (:activeMonitor @protocol) nil
                                             (fn [key atom old-state new-state] (callback x) (remove-watch (:activeMonitor @protocol) nil))))
                                (do
                                  (activateNextMonitor action from to protocol)
                                  (callback x)
                                  (closeProtocol! protocol)))
                              (do
                                (incorrectCommunication (format "recv action: %s is not allowed to proceed from %s to %s" action from to))
                                (callback nil)))))))]
    (if (nil? channel)
      (incorrectCommunication "Cannot find channel from %s to %s in the defined channels of the protocol! Please make sure you supply supported sender and receiver pair")
      (do
        (reset! (:receivingQueue channel) (conj @(:receivingQueue channel) f))
        (when (isReceiveMActive? action from to protocol)
          (activateMonitorOnReceive protocol))
        ))))