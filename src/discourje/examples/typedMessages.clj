(ns discourje.examples.typedMessages
  (require [discourje.core.async :refer :all]
           [discourje.core.logging :refer :all]))

;This function will generate a vector with 4 interactions to send and receive the greet message.
;Notice how we use a type as label for the interaction.
(def message-exchange-pattern
  (mep (-->> String "alice" "bob")
       (-->> String "bob" "alice")
       (-->> String "alice" "carol")
       (-->> String "carol" "alice")
       (close "alice" "bob")
       (close "bob" "alice")
       (close "alice" "carol")
       (close "carol" "alice")))

;setup infrastructure, generate channels and add monitor
(def infrastructure (add-infrastructure message-exchange-pattern))
;Get the channels
(def alice-to-bob (get-channel infrastructure "alice" "bob"))
(def bob-to-alice (get-channel infrastructure "bob" "alice"))
(def alice-to-carol (get-channel infrastructure "alice" "carol"))
(def carol-to-alice (get-channel infrastructure "carol" "alice"))

(defn- greet-bob-and-carol
  "This function will use the protocol to send the greet message to bob and carol.
  Note: we do not send data of type message but simply a string.
  Discourje will generate a message ->message{:label String :content Greetings, from alice!}"
  []
  (>!! alice-to-bob "Greetings, from alice!")
  (log-message (<!! bob-to-alice))
  (>!! alice-to-carol "Greetings, from alice!")
  (log-message (<!! carol-to-alice))
  (close! alice-to-bob)
  (close! bob-to-alice)
  (close! alice-to-carol)
  (close! carol-to-alice))

(defn- receive-greet
  "This function will use the protocol to listen for the greet message."
  [input-channel output-channel]
  (let [message (<!! input-channel)]
    (log-message (format "Received message: %s to %s" message (get-consumer input-channel)))
    (>!! output-channel  "Hi to you too!")))

;start the `greet-bob-and-carol' function on thread
(clojure.core.async/thread (greet-bob-and-carol))
;start the `receive-greet' function on thread and add the channels
(clojure.core.async/thread (receive-greet alice-to-bob bob-to-alice))
;start the `receive-greet' function on thread and add the channels
(clojure.core.async/thread (receive-greet alice-to-carol carol-to-alice))