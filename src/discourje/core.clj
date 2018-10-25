(ns discourje.core
  (:require [clojure.core.async :as async :refer :all]
            [clojure.core :refer :all]))

(defprotocol messenger
  (provide
    [this message]
    [this message operation])
  (consume
  ;  [this message]
    [this operation]))

;use take! to also supply a callback when a message is received
(defn putMessage [channel message]
  (go (>! channel message)))

(defn takeMessage [channel]
   (go (<! channel)))

(defrecord participant [thread input output]
  messenger
  (provide [this message] (putMessage output message))
  (provide [this message operation] (putMessage output (operation message)))
  (consume [this operation] (go (operation (<! input)))))
