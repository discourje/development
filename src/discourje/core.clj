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
  "Define channel interface"
  (send [source sink message] "Send from source to sink the following message "))