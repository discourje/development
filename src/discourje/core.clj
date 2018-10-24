(ns discourje.core
  (:require [clojure.core.async :as async :refer :all]
            [clojure.core :refer :all]))

(defprotocol messenger
  (provide [this channel message])
  (consume [this message]))

;use take! to also supply a callback when a message is received
(defn putMessage [channel message]
  (go (>! channel message)))

(defn takeMessage [channel]
   (go (<! channel)))

(defrecord participant [thread input output]
  messenger
  (provide [this channel message] (putMessage channel message))
  (consume [this message] (takeMessage input)))
