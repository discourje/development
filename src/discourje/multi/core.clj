(ns discourje.multi.core
  (:require [clojure.core.async :as async :refer :all]
            [clojure.core :refer :all])
  (use [discourje.multi.monitor :only [incorrectCommunication isCommunicationValid? activateNextMonitor]])
  (:import (clojure.lang Seqable)))

;Defines a communication channel with a sender, receiver (strings) and a channel Async.Chan.
(defrecord communicationChannel [sender receiver channel])

(defn- generateChannel
  "function to generate a channel between sender and receiver"
  [sender receiver]
  (->communicationChannel sender receiver (chan)))

(defn uniqueCartesianProduct
  "Generate channels between all participants and filter out duplicates e.g.: buyer1<->buyer1"
  [x y]
  (filter some?
          (for [x x y y]
            (when (not (identical? x y))
              (vector x y)))))

(defn generateChannels
  "Generates communication channels between all participants"
  ([participants]
   (map #(apply generateChannel %) (uniqueCartesianProduct participants participants)))
  ([participants test]

    ))

(defn putMessage
  "Puts message on the channel, non-blocking"
  [channel message]
  (println (format "setting message %s" message))
  (println channel)
  (put! channel message))

(defn blockingTakeMessage
  "Takes message from the channel, blocking"
  [channel]
  (<!! channel))


(defn getChannel
  "finds a channel based on sender and receiver"
  [sender receiver channels]
  (println (format "Sender: %s and receiver(s) %s" sender receiver))
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
  [action value from to protocol]
  (if (nil? (:activeMonitor @protocol))
    (incorrectCommunication "protocol does not have a defined channel to monitor! Make sure you supply send! with an instantiated protocol!")
    (if (isCommunicationValid? action from to protocol)
      (let [currentMonitor @(:activeMonitor @protocol)]
        (if (vector? (:to currentMonitor))
          (do
            (println "yes this one is seqable")
            (println (:to currentMonitor))
            (for [receiver (:to currentMonitor)]
              (do(println (format "looping through current monitor To %s" receiver))
              (allowSend (:channel (getChannel (:from currentMonitor) receiver (:channels @protocol))) value))))
          (do (println "oh yes sending")
     ;   (activateNextMonitor protocol) ;only activate new monitor when receive action has finished
        (println (format "allowing send to channel %s" (:to currentMonitor)))
        (allowSend (:channel (getChannel (:from currentMonitor) (:to currentMonitor) (:channels @protocol))) value))))
      (incorrectCommunication (format "Send action: %s is not allowed to proceed from %s to %s" action from to)))))

(defn recv!
  "receive something through the protocol"
  [action from to protocol callback]
  (let [channel (getChannel from to (:channels @protocol))]
    (println "")
    (println "started take with callback")
    (println channel)
    (if (nil? channel)
      (incorrectCommunication "Cannot find channel from %s to %s in the defined channels of the protocol! Please make sure you supply supported sender and receiver pair")
      (take! (:channel channel)
             (fn [x]
               (println "taking!!!")
               (println channel)
               (if (nil? (:activeMonitor @protocol))
                 (incorrectCommunication "protocol does not have a defined channel to monitor! Make sure you supply send! with an instantiated protocol!")
                 (if (isCommunicationValid? action from to protocol)
                   (let [currentMonitor @(:activeMonitor @protocol)]
                     (println "oh yes recv")
                     (activateNextMonitor protocol)
                     (println callback)
                     (callback x))
                   (do
                     (incorrectCommunication (format "recv action: %s is not allowed to proceed from %s to %s" action to from))
                     (callback nil))
                   )))))))