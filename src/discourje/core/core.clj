(ns discourje.core.core
  (:require [clojure.core.async :as async :refer :all]
            [clojure.core :refer :all]
            [discourje.core.dataStructures :refer :all])
  (use [discourje.core.monitor :only [closeProtocol! activateMonitorOnSend isCommunicationValid? activateNextMonitor hasMultipleReceivers? removeReceiver getTargetBranch]]
       [discourje.core.validator :only [log-error log-message]])
  (:import (discourje.core.dataStructures choice sendM receiveM)))

(defn putMessage
  "Puts message on the channel, non-blocking"
  [channel message]
  ;(log-message (format "setting message %s" message))
  (put! channel message))

(defn getChannel
  "Finds a channel based on sender and receiver"
  [sender receiver channels]
  (first
    (filter (fn [ch]
              (and
                (= (:sender ch) sender)
                (= (:receiver ch) receiver)))
            channels)))

(defn- allowSend
  "Send is allowed to put on the channel of the active monitor"
  [channel value]
  (if (vector? channel)
    (for [receiver channel] (putMessage receiver value))
    (putMessage channel value)))

(defn incorrectCommunication
  "Log invalid communication."
  [type message]
  (log-error type message))

(defn dcj-send!
  "Send something through the protocol"
  ([action value from to protocol]
   (if (nil? (:activeMonitor @protocol))
     (incorrectCommunication :monitor-nil "Protocol does not have a defined channel to monitor! Make sure you supply send! with an instantiated protocol!")
     (do (when (not (and (isCommunicationValid? action from to :send protocol) (not (instance? receiveM @(:activeMonitor @protocol)))))
           (incorrectCommunication :invalid-communication (format "Send action: %s is not allowed to proceed from %s to %s. Current ActiveMonitor: %s" action from to (to-string @(:activeMonitor @protocol)))))
         (let [currentMonitor @(:activeMonitor @protocol)]
           ;(log-message "yes sending: " action)
           (cond
             (instance? sendM currentMonitor)
             (do (activateMonitorOnSend action from to protocol)
                 (dcj-send! currentMonitor value protocol))
             (instance? choice currentMonitor)
             (let [target (getTargetBranch action from to :send protocol)]
               (if (nil? target)
                 (incorrectCommunication :monitor-nil "Protocol does not have a defined channel to monitor after checking CHOICE branch! Make sure you supply send! with an instantiated protocol!")
                 (do
                 (when (not (instance? sendM target))
                 (incorrectCommunication :invalid-communication (format "Target choice is not a sendM, but is %s" (to-string target))))
               (activateMonitorOnSend action from to protocol)
               (dcj-send! target value protocol)))))))))
  ([currentMonitor value protocol]
   (if (vector? (:to currentMonitor))
     (doseq [receiver (:to currentMonitor)]
       (allowSend (:channel (getChannel (:from currentMonitor) receiver (:channels @protocol))) value))
     (allowSend (:channel (getChannel (:from currentMonitor) (:to currentMonitor) (:channels @protocol))) value))))

(defn dcj-recv!
  "Receive something through the protocol"
  ([action from to protocol callback]
   (let [channel (getChannel from to (:channels @protocol))]
     (if (nil? channel)
       (incorrectCommunication :undefined-channel (format "Cannot find channel from %s to %s in the defined channels of the protocol! Please make sure you supply supported sender and receiver pair" from to))
       (take! (:channel channel)
              (fn [x]
            ;    (log-message "recv! got " x)
                (if (nil? (:activeMonitor @protocol))
                  (incorrectCommunication :monitor-nil "protocol does not have a defined channel to monitor! Make sure you supply recv! with an instantiated protocol!")
                  (do (when (not (and (isCommunicationValid? action from to :receive protocol) (not (instance? sendM @(:activeMonitor @protocol)))))
                        (incorrectCommunication :invalid-communication
                                                (format "recv action: %s is not allowed to proceed from %s to %s. Current monitor: %s"
                                                        action from to (to-string @(:activeMonitor @protocol)))))
                      (let [currentMonitor @(:activeMonitor @protocol)]
                        (cond
                          (instance? receiveM currentMonitor)
                          (dcj-recv! action from to protocol callback x)
                          (instance? choice currentMonitor)
                          (let [target (getTargetBranch action from to :receive protocol)]
                            (if (nil? target)
                              (incorrectCommunication :monitor-nil "Protocol does not have a defined channel to monitor after checking CHOICE branch! Make sure you supply recv! with an instantiated protocol!")
                              (do
                            (when (not (instance? receiveM target))
                              (incorrectCommunication :invalid-communication (format "target choice is not a receiveM. But is: %s" (to-string target))))
                            (dcj-recv! action from to protocol callback x target)))))))))))))
  ([action from to protocol callback value]
   (dcj-recv! action from to protocol callback value (:activeMonitor @protocol)))
  ([action from to protocol callback value targetM]
  ; (log-message targetM)
   (if (hasMultipleReceivers? protocol)
     (do (removeReceiver protocol to)
         (add-watch targetM nil (fn [_ atom old-state new-state]
                                  (when (and
                                          (not= (:action old-state) (:action new-state))
                                          (not= (:from old-state) (:from new-state)))
                                    (do (remove-watch atom nil)
                                    (callback value)))))
         )
     (do (activateNextMonitor action from to :receive protocol)
         (callback value)
         (closeProtocol! protocol)))))

(defrecord participant [name protocol]
  role
  (send-to [this action value to] (dcj-send! action value name to protocol))
  (receive-by [this action from callback] (dcj-recv! action from name protocol callback)))