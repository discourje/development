(ns discourje.core.core
  (:require [clojure.core.async :as async :refer :all]
            [clojure.core :refer :all]
            [discourje.core.dataStructures :refer :all])
  (use [discourje.core.monitor :only [incorrectCommunication closeProtocol! activateMonitorOnSend isCommunicationValid? activateNextMonitor hasMultipleReceivers? removeReceiver getTargetBranch]])
  (:import (discourje.core.dataStructures choice sendM receiveM)))

(defn putMessage
  "Puts message on the channel, non-blocking"
  [channel message]
  ;(println (format "setting message %s" message))
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
         ;(println "yes sending: " action)
         (cond
           (instance? sendM currentMonitor)
           (do (activateMonitorOnSend action from to protocol)
             (send! currentMonitor value protocol))
           (instance? choice currentMonitor)
           (let [target (getTargetBranch action from to protocol)]
             (if (instance? sendM target)
             (do (activateMonitorOnSend action from to protocol)
               (send! target value protocol))
             (println "target choice is not a sendM")
             ))))
       (incorrectCommunication (format "Send action: %s is not allowed to proceed from %s to %s" action from to)))))
  ([currentMonitor value protocol]
   (if (vector? (:to currentMonitor))
     (doseq [receiver (:to currentMonitor)]
       (allowSend (:channel (getChannel (:from currentMonitor) receiver (:channels @protocol))) value))
       (allowSend (:channel (getChannel (:from currentMonitor) (:to currentMonitor) (:channels @protocol))) value))))

(defn recv!
  "receive something through the protocol"
  ([action from to protocol callback]
   (let [channel (getChannel from to (:channels @protocol))]
     (if (nil? channel)
       (incorrectCommunication "Cannot find channel from %s to %s in the defined channels of the protocol! Please make sure you supply supported sender and receiver pair")
         (take! (:channel channel)
              (fn [x]
                ;(println "recv! got " x)
                (if (nil? (:activeMonitor @protocol))
                  (incorrectCommunication "protocol does not have a defined channel to monitor! Make sure you supply recv! with an instantiated protocol!")
                  (if (and (isCommunicationValid? action from to protocol) (not (instance? sendM @(:activeMonitor @protocol))))
                    (let [currentMonitor @(:activeMonitor @protocol)]
                      (cond
                        (instance? receiveM currentMonitor)
                        (recv! action from to protocol callback x)
                        (instance? choice currentMonitor)
                        (let [target (getTargetBranch action from to protocol)]
                          (if (instance? receiveM target)
                            (recv! action from to protocol callback x target)
                            (println "target choice is not a receiveM" target))
                          )))
                    (do
                      (incorrectCommunication (format "recv action: %s is not allowed to proceed from %s to %s___Current monitor: Type: %s Action: %s, From: %s To: %s" action from to @(:activeMonitor @protocol) (:action @(:activeMonitor @protocol)) (:from @(:activeMonitor @protocol)) (:to @(:activeMonitor @protocol))))
                      (callback nil)))))))))
  ([action from to protocol callback value]
   (if (hasMultipleReceivers? protocol)
     (do
       (removeReceiver protocol to)
       (let [activeMonitor (:activeMonitor @protocol)
             activeMonitorRef @activeMonitor]
         (add-watch (:activeMonitor @protocol) nil
                    (fn [key atom old-state new-state]
                      (when (and
                              (not= (:action activeMonitorRef) (:action new-state))
                              (not= (:from activeMonitorRef) (:from new-state)))
                        (remove-watch activeMonitor nil)
                        (callback value))))))
     (do
       (activateNextMonitor action from to protocol)
       (callback value)
       (closeProtocol! protocol)))
    )
  ([action from to protocol callback value targetM]
   (if (hasMultipleReceivers? protocol)
     (do
       (removeReceiver protocol to)
       (let [targetMRef @targetM]
         (add-watch (:activeMonitor @protocol) nil
                    (fn [key atom old-state new-state]
                      (when (and
                              (not= (:action targetMRef) (:action new-state))
                              (not= (:from targetMRef) (:from new-state)))
                        (remove-watch targetM nil)
                        (callback value))))))
     (do
       (activateNextMonitor action from to protocol)
       (callback value)
       (closeProtocol! protocol)))))

(defrecord participant [name protocol]
  role
  (send-to [this action value to] (send! action value name to protocol))
  (receive-by [this action from callback] (recv! action from name protocol callback)))