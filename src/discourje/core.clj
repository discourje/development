(ns discourje.core
  (:require [clojure.core.async :as async :refer :all]
            [clojure.core :refer :all]))

(defprotocol messenger
  (provide
    [this message]
    [this message operation])
  (consume
  ;  [this message]
    [this operation]
    [this operation test]))

;use take! to also supply a callback when a message is received
(defn putMessage [channel message]
  (go (>! channel message)))

(defn takeMessage [channel]
   (go (<! channel)))

(defrecord participant [threade input output]
  messenger
  (provide [this message] (putMessage output message))
  (provide [this message operation] (putMessage output (operation message)))
  (consume [this operation] (go (operation (<! input))))
  (consume [this operation test] (go (operation (<! (thread (<!! (go (<! input)))))))))

(defn createParticipant
  "Create a new participant, simulates constructor-like behavior"
  []
  (->participant (thread) (chan) (chan)))