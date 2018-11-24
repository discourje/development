(ns discourje.multiparty.core
  (:require [clojure.core.async :as async :refer :all]
            [clojure.core :refer :all]))


(defrecord communicationChannel [sender receiver channel])

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

;(defn send!
;  "send something through the protocol"
;  [action value from to]
;  (discourje.multiparty.TwoBuyersProtocol/communicate action value from to))
;
;(defn receive!
;  "receive something through the protocol"
;  [action from to]
;  (discourje.multiparty.TwoBuyersProtocol/communicate action from to))
