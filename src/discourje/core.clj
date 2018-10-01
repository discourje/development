(ns discourje.core
  (:require [clojure.core :refer :all]))
;add dcj prefix to remove confusion of native clojure constructs ( but should be removed once working properly).

(defprotocol dcj-source
  "A participant identified as a Sender."
  (dcj-send [message] "The message to send"))

(defprotocol dcj-sink
  "A participant identified as a Receiver."
  (dcj-receive [message] "Receive a message"))

(defprotocol dcj-channel
  "Define channel interface with overloads(arity)"
  (transmit [chan dcj-source dcj-sink message]))

(defn addMessage [])