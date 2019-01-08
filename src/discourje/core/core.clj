(ns discourje.core.core
  (:require [clojure.core.async :as async :refer :all]
            [clojure.core :refer :all]
            [discourje.core.dataStructures :refer :all])
  (use [discourje.core.monitor :only [incorrectCommunication closeProtocol! activateMonitorOnSend isCommunicationValid? activateNextMonitor hasMultipleReceivers? removeReceiver getTargetBranch]])
  (:import (discourje.core.dataStructures choice sendM receiveM)))

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
     (if (and (isCommunicationValid? action from to protocol) (not (instance? receiveM @(:activeMonitor @protocol))))
       (let [currentMonitor @(:activeMonitor @protocol)]
         (cond
           (instance? sendM currentMonitor)
           (do

             (activateMonitorOnSend action from to protocol)
             (send! currentMonitor value protocol)
               )
           (instance? choice currentMonitor)
           (let [target (getTargetBranch action from to protocol)]
             (instance? sendM target)
             (do

               (activateMonitorOnSend action from to protocol)
               (send! target value protocol)

                 ))))
       (do
         (println "_______________________")
         (println @(:activeMonitor @protocol))
         (incorrectCommunication (format "Send action: %s is not allowed to proceed from %s to %s" action from to))))))
  ([currentMonitor value protocol]
   (if (vector? (:to currentMonitor))
     (doseq [receiver (:to currentMonitor)]
       (allowSend (:channel (getChannel (:from currentMonitor) receiver (:channels @protocol))) value))
     (allowSend (:channel (getChannel (:from currentMonitor) (:to currentMonitor) (:channels @protocol))) value))))

(defn recv!
  "receive something through the protocol"
  [action from to protocol callback]
  (let [channel (getChannel from to (:channels @protocol))]
    (if (nil? channel)
      (incorrectCommunication "Cannot find channel from %s to %s in the defined channels of the protocol! Please make sure you supply supported sender and receiver pair")
      (take! (:channel channel)
             (fn [x]
               (if (nil? (:activeMonitor @protocol))
                 (incorrectCommunication "protocol does not have a defined channel to monitor! Make sure you supply send! with an instantiated protocol!")
                 (if (isCommunicationValid? action from to protocol)
                   (if (hasMultipleReceivers? protocol)
                     (do
                       (removeReceiver protocol to)
                       (add-watch (:activeMonitor @protocol) nil
                                  (fn [key atom old-state new-state]
                                    (remove-watch (:activeMonitor @protocol) nil)
                                    (callback x)
                                    )))
                     (do
                       (activateNextMonitor action from to protocol)
                       (callback x)
                       (closeProtocol! protocol)))
                   (do
                     (println "++++++++++++++++++++++++")
                     (println (:activeMonitor @protocol) )
                     (incorrectCommunication (format "recv action: %s is not allowed to proceed from %s to %s" action from to))
                     (callback nil)))))))))
;
;(defn recvDelayed!
;  "receive something through the protocol"
;  [action from to protocol callback]
;  (let [channel (getChannel from to (:channels @protocol))
;        f (fn [] (take! (:channel channel)
;                        (fn [x]
;                          (if (nil? (:activeMonitor @protocol))
;                            (incorrectCommunication "protocol does not have a defined channel to monitor! Make sure you supply send! with an instantiated protocol!")
;                            (if (isCommunicationValid? action from to protocol)
;                              (if (hasMultipleReceivers? protocol)
;                                (do
;                                  (removeReceiver protocol to)
;                                  (let [rec (:activeMonitor @protocol)]
;                                    (add-watch rec nil
;                                               (fn [key atom old-state new-state]
;                                                 (remove-watch rec nil)
;                                                 (callback x)))))
;                                (do (activateNextMonitor action from to protocol)
;                                    (callback x)
;                                    (closeProtocol! protocol)))
;                              (do
;                                (incorrectCommunication (format "recv action: %s is not allowed to proceed from %s to %s" action from to))
;                                (callback nil)))))))]
;    (if (nil? channel)
;      (incorrectCommunication "Cannot find channel from %s to %s in the defined channels of the protocol! Please make sure you supply supported sender and receiver pair")
;      (do
;        (reset! (:receivingQueue channel) (conj @(:receivingQueue channel) f))
;        (when (isReceiveMActive? action from to protocol)
;          (activateMonitorOnReceive protocol))))))

(defrecord participant [name protocol]
  role
  (send-to [this action value to] (send! action value name to protocol))
  (receive-by [this action from callback] (recv! action from name protocol callback)))