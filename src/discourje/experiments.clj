(ns discourje.experiments
  (:refer-clojure :exclude [send]))
;; defrecord & deftype does not support a doc string, maybe write custom macro that does?

(defrecord message [data])
(deftype channel [sender receiver message])

(defprotocol source
  "A participant identified as a Sender."
  (send [channel] "Send something through the channel"))

(defprotocol sink
  "A participant identified as a Receiver."
  (receive [channel] "Receive something from a channel"))

(deftype sender [name channel]
  source
  (send [channel]
    (:data (message. channel) )))

