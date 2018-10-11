(ns discourje.twoBuyerProtocol.pipes.participant
  (:require [clojure.core.async :as async :refer :all]
            [discourje.twoBuyerProtocol.pipes.util :refer :all]))


(defprotocol stakeHolder
  (receive [this message])
  (provide [this channel message tag])
  (getChannel [this]))


(defrecord channelHandler [C]
  stakeHolder
  (receive [this message] (str "test"))
  (provide [this channel message tag] (putOnChannel channel message tag))
  (getChannel [this] C))

(def a (->channelHandler (chan)))
(defn generateParticipant [identifier channel]
  (def identifier (->channelHandler channel)))


(defn participant [name]
  (def name (chan)))
