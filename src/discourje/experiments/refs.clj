(ns discourje.experiments.refs)

(defrecord message [data])
(->message "hello")

(defprotocol source
  "A participant identified as a Sender."
  (se [message] "The message to send"))

(defprotocol sink
  "A participant identified as a Receiver."
  (re [message] "Receive a message"))

(defrecord participant [name]
  source
  (se [message] (:data message))
  sink
  (re [message] (:data message)))

(def alice (ref (->participant "alice")))
(def bob (ref (->participant "bob")))

(def channel (ref ()))

(defn sendMessage [source sink message]
  (dosync (alter channel conj message)  ))

(sendMessage alice bob (->message "Hi there"))
(sendMessage alice bob (->message "Hi there again"))
(sendMessage alice bob (->message "Hi there again and again"))