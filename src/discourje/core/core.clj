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

(defn dcj-send!
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
               (dcj-send! currentMonitor value protocol))
           (instance? choice currentMonitor)
           (let [target (getTargetBranch action from to protocol)]
             (if (instance? sendM target)
               (do (activateMonitorOnSend action from to protocol)
                   (dcj-send! target value protocol))
               (println "target choice is not a sendM")
               ))))
       (incorrectCommunication (format "Send action: %s is not allowed to proceed from %s to %s" action from to)))))
  ([currentMonitor value protocol]
   (if (vector? (:to currentMonitor))
     (doseq [receiver (:to currentMonitor)]
       (allowSend (:channel (getChannel (:from currentMonitor) receiver (:channels @protocol))) value))
     (allowSend (:channel (getChannel (:from currentMonitor) (:to currentMonitor) (:channels @protocol))) value))))

(defn dcj-recv!
  "receive something through the protocol"
  ([action from to protocol callback]
   (let [channel (getChannel from to (:channels @protocol))]
     (if (nil? channel)
       (incorrectCommunication (format "Cannot find channel from %s to %s in the defined channels of the protocol! Please make sure you supply supported sender and receiver pair" from to))
       (take! (:channel channel)
              (fn [x]
                ;(println "recv! got " x)
                (if (nil? (:activeMonitor @protocol))
                  (incorrectCommunication "protocol does not have a defined channel to monitor! Make sure you supply recv! with an instantiated protocol!")
                  (if (and (isCommunicationValid? action from to protocol) (not (instance? sendM @(:activeMonitor @protocol))))
                    (let [currentMonitor @(:activeMonitor @protocol)]
                      (cond
                        (instance? receiveM currentMonitor)
                        (dcj-recv! action from to protocol callback x)
                        (instance? choice currentMonitor)
                        (let [target (getTargetBranch action from to protocol)]
                          (if (instance? receiveM target)
                            (dcj-recv! action from to protocol callback x target)
                            (println "target choice is not a receiveM" target))
                          )))
                    (do
                      (incorrectCommunication (format "recv action: %s is not allowed to proceed from %s to %s___Current monitor: Type: %s Action: %s, From: %s To: %s" action from to @(:activeMonitor @protocol) (:action @(:activeMonitor @protocol)) (:from @(:activeMonitor @protocol)) (:to @(:activeMonitor @protocol))))
                      (callback nil)))))))))
  ([action from to protocol callback value]
   (dcj-recv! action from to protocol callback value (:activeMonitor @protocol)))
  ([action from to protocol callback value targetM]
   (if (hasMultipleReceivers? protocol)
     (do
       (removeReceiver protocol to)
       (add-watch targetM nil (fn [_ atom old-state new-state]
           (when (and
                   (not= (:action old-state) (:action new-state))
                   (not= (:from old-state) (:from new-state)))
             (remove-watch atom nil)
             (callback value))))
       )
     (do
       (activateNextMonitor action from to protocol)
       (callback value)
       (closeProtocol! protocol)))))

(defrecord participant [name protocol]
  role
  (send-to [this action value to] (dcj-send! action value name to protocol))
  (receive-by [this action from callback] (dcj-recv! action from name protocol callback)))