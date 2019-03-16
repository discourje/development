(ns discourje.core.async.examples.customChannels
  (require [discourje.core.async.async :refer :all]
           [discourje.core.async.logging :refer :all]))

(defn- define-sequence-protocol
  "This function will generate a vector with 2 interactions to send and receive the greet message.
  Notice how send and receivers are defined separately in order to allow for sequencing of actions!"
  []
  (create-protocol [(make-interaction "greet" "alice" "bob")
                    (make-interaction "greet" "alice" "carol")]))

;Define custom channels, which differ in buffer size (1 and 2)
(def a->b (generate-channel "alice" "bob" 1))
(def a->c (generate-channel "alice" "carol" 2))

;setup infrastructure, generate channels and add monitor, notice that we supply the generate-infrastructure function with custom channels vector
;generate-infrastructure will detect if all channels in the vector implement the transportable defprotocol and that all channels required for the protocol are present in the custom channel vector
(def infrastructure (generate-infrastructure (define-sequence-protocol) [a->b a->c]))
;Get the channels
(def alice-to-bob (get-channel "alice" "bob" infrastructure))
(def alice-to-carol (get-channel "alice" "carol" infrastructure))

(defn- greet-bob-and-carol
  "This function will use the protocol to send the greet message to bob and carol."
  []
  (>!! alice-to-bob (->message "greet" "Greetings, from alice!"))
  (>!! alice-to-carol (->message "greet" "Greetings, from alice!")))

(defn- receive-greet
  "This function will use the protocol to listen for the greet message."
  [channel]
  (let [message (<!! channel "greet")]
        (log-message (format "Received message: %s to %s" (get-content message) (get-consumer channel)))))

;start the `greet-bob-and-carol' function on thread
(clojure.core.async/thread (greet-bob-and-carol))
;start the `receive-greet' function on thread and add the channel
(clojure.core.async/thread (receive-greet alice-to-bob))
;start the `receive-greet' function on thread and add the channel
(clojure.core.async/thread (receive-greet alice-to-carol))
