(ns discourje.multiparty.TwoBuyersProtocol
  (:require [discourje.multiparty.core :refer :all]
    ;[discourje.multiparty.Buyer1 :refer :all]
    ;[discourje.multiparty.Buyer2 :refer :all]
    ;[discourje.multiparty.Seller :refer :all]
            ))

(def channels (generateChannels ["buyer1" "buyer2" "seller"]))

(defn getChannel
  "finds a channel based on sender and receiver"
  [sender receiver]
  (first
    (filter (fn [ch]
              (and
                (= (:sender ch) sender)
                (= (:receiver ch) receiver)))
            channels)))


;define a monitor to check communication, this will be used to verify correct conversation
(defrecord monitor [action from to])
(defrecord choice [trueChannel falseChannel trueBranch falseBranch])


(defn- defineProtocol []
  (vector
    (->monitor "title" "buyer1" "seller")
    (->monitor "quote" "seller" {"buyer1" "buyer2"})
    (->monitor "quoteDiv" "buyer1" "buyer2")
    (->choice (->monitor "ok" "buyer2" "seller") (->monitor "quit" "buyer2" "seller")
              [(->monitor "address" "buyer2" "seller")
               (->monitor "date" "seller" "buyer2")]
              [(->monitor "quit" "buyer2" "seller")])))

(def protocol (atom (defineProtocol)))

(def activeMonitor (atom {}))

(defn setActiveMonitor
  "Set the active monitor"
  []
  (let [current (first @protocol)]
    (reset! activeMonitor current)
    (reset! protocol (subvec @protocol 1))
    ))

(defn allowSend
  "send is allowed to put it on the channel of the active monitor"
  [channel value]
  (if (instance? clojure.lang.Seqable channel)
    (for [receiver channel] (putMessage receiver value))
  (putMessage channel value))

(defn allowReceive
  "receive is allowed to take from the channel of the active monitor"
  [channel]
  (blockingTakeMessage channel))


(defn validCommunication?
  "Checks if communication is valid by comparing input to the active monitor"
  [action from to]
  (cond
    (instance? monitor @activeMonitor)
    (do
      (println "yes is monitor")
      (println (:to @activeMonitor))
      (and
        (= action (:action @activeMonitor))
        (= from (:from @activeMonitor))
        (and (if (instance? clojure.lang.Seqable (:to @activeMonitor))
               (contains? (:to @activeMonitor) to)
               (= to (:to @activeMonitor))))))
    (instance? choice @activeMonitor)
    (do
      (println "yes is choice"))))

(defn incorrectCommunication
  "communication incorrect, log a message! (or maybe throw exception)"
  [message]
  (println message))

(setActiveMonitor)

(defn communicate
  ([action value from to]
   (println "sending")
   (if (nil? @activeMonitor)
     (incorrectCommunication "protocol does not have a defined channel to monitor!")
     (if (validCommunication? action from to)
       (let [currentMonitor @activeMonitor]
         (println "oh yes")
         (setActiveMonitor)
         (allowSend (:channel (getChannel (:from currentMonitor) (:to currentMonitor))) value)
         )
       (incorrectCommunication "protocol does not allow diverging from communication channels!"))))
  ([action from to]
   (println "listening")
   (if (nil? @activeMonitor)
     (incorrectCommunication "protocol does not have a defined channel to monitor!")
     (if (validCommunication? action from to)
       (let [value (allowReceive (:channel (getChannel (:from @activeMonitor) (:to @activeMonitor))))]
         (setActiveMonitor)
         value)
       (incorrectCommunication "protocol does not allow diverging from communication channels!")))))