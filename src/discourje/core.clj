(ns discourje.core
  (:refer-clojure :exclude [send]))

(defrecord message [data])

(defprotocol source
  "A participant identified as a Sender."
  (send [message] "The message to send"))

(defprotocol sink
  "A participant identified as a Receiver."
  (receive [message] "Receive a message"))

(defprotocol channel
  "Define channel interface with overloads(arity)"
  (transmit
    [source sink message]                                   ;"transmit from source to sink the following message"
    [source sink message method]))                           ;"transmit from source to sink the following message executing the desire method"

;participant with source and sink template
(defrecord participant [name]
  source
    (send [sendableMessage] (:data sendableMessage))
  sink
    (receive [receivableMessage] (:data receivableMessage)))

(defprotocol protocol [name])