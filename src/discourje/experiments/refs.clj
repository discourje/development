(ns discourje.experiments.refs)

(defrecord message [data])
(->message "hello")

(defprotocol source
  "A participant identified as a Sender."
  (se [source message] "The message to send"))

(defprotocol sink
  "A participant identified as a Receiver."
  (re [sink message] "Receive a message"))

(defrecord participant [name]
  source
  (se [this message] (format "Message send from: %s, " (:name this)))
  sink
  (re [this message] (format "%s received a message: %s" (:name this) (:data message))))

(def alice (ref (->participant "alice")))
(def bob (ref (->participant "bob")))

(def channel (ref ""))

(defn sendMessage [source sink message]
  (dosync
    (ref-set channel
             (let [so @source
                   si @sink
                   me message]
               (re si
                   (->message
                     (format "%s %s" (se so me) me)))))))

(sendMessage alice bob "Hi there bob")
(sendMessage bob alice "Hi there too alice")
(sendMessage alice bob "Hi there again")