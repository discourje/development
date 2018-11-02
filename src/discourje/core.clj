(ns discourje.core
  (:require [clojure.core.async :as async :refer :all]
            [clojure.core :refer :all]))

(defprotocol messenger
  (provide
    [this message]
    [this message operation])
  (consume
    [this operation]
    [this operation test])
  (dataFromOutput
    [this])
  (dataToInput
    [this message])
  (functionToInput
    [this operation]))

;use take! to also supply a callback when a message is received
(defn putMessage [channel message]
  (go (>! channel message)))

(defn blockingPutMessage [channel message]
  (>!! channel message))

(defn takeMessage [channel]
   (go (<! channel)))

(defn blockingTakeMessage [channel]
  (<!! channel))

(defrecord participant [input output state]
  messenger
  (provide [this message] (putMessage output message))
  (provide [this message operation] (putMessage output (operation message)))
  (consume [this operation] (go (operation (<! input))))
  (consume [this operation test] (go (operation (<! (thread (<!! (go (<! input))))))))
  ;new Variants-blocking variants
  (dataFromOutput [this] (blockingTakeMessage output))
  (dataToInput [this message] (putMessage input message))
  (functionToInput [this function] (putMessage input (function))))

(defn fromOutputToInput
  "Take data from input channel FROM and put it to output channel TO"
  [from to]
  (go (>! (:input to) (<! (:output from)))))

(defn fromInputToOutput
  "Take data from output channel FROM and put it to input channel TO"
  [from to]
  (go (>! (:output to) (<! (:input from)))))

(defn createParticipant
  "Create a new participant, simulates constructor-like behavior"
  []
  (atom (->participant (chan) (chan) nil)))


(defn sendMessage
  "Send message from FROM to TO & MORE"
  [message from to & more]
  (provide from message)
  (fromOutputToInput from to)
  (for [receiver more] (fromOutputToInput from receiver)))