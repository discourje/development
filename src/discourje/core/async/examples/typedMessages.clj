(ns discourje.core.async.examples.typedMessages
  (require [discourje.core.async.async :refer :all]
           [discourje.core.async.logging :refer :all]))

(defn- define-typed-sequence-protocol
  "This function will generate a vector with 2 interactions to send and receive the greet message.
  Notice how we use a type as label for the interaction."
  []
  (create-protocol [(make-interaction String "alice" "bob")
                    (make-interaction String "alice" "carol")]))

;setup infrastructure, generate channels and add monitor
(def infrastructure (generate-infrastructure (define-typed-sequence-protocol)))
;Get the channels
(def alice-to-bob (get-channel "alice" "bob" infrastructure))
(def alice-to-carol (get-channel "alice" "carol" infrastructure))

(defn- greet-bob-and-carol
  "This function will use the protocol to send the greet message to bob and carol.
  Notice how we only send content of a message through the protocol.
  Discourje will automatically generate a message with {:label type-of message content (in this case: String) :content original data (in this case: Greetings, from alice!)}
  So the send message will look like: Message{:label String :content Greetings, from alice!}"
  []
  (>!! alice-to-bob "Greetings, from alice!")
  (>!! alice-to-carol "Greetings, from alice!"))

(defn- receive-greet
  "This function will use the protocol to listen for the greet message.
  Notice how we receive by giving type as label"
  [channel]
  (let [message (<!! channel String)]
        (log-message (format "Received message: %s to %s" (get-content message) (get-consumer channel)))))

;start the `greet-bob-and-carol' function on thread
(clojure.core.async/thread (greet-bob-and-carol))
;start the `receive-greet' function on thread and add the channel
(clojure.core.async/thread (receive-greet alice-to-bob))
;start the `receive-greet' function on thread and add the channel
(clojure.core.async/thread (receive-greet alice-to-carol))
