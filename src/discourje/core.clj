(ns discourje.core)
;; defrecord & deftype does not support a doc string, maybe write custom macro that does?

(defrecord message [data])

(deftype channel [sender receiver message])

(defprotocol source
  "A participant identified as a Sender."
  (verzend [channel] "Send something through the channel"))

(defprotocol sink
  "A participant identified as a Receiver."
  (ontvang [channel] "Receive something from a channel"))

(deftype sender [name channel]
  source
  (verzend [channel] (println (.message channel))))
