(ns discourje.core.async.examples.sequencing
  (require [discourje.core.async.async :refer :all]
           [discourje.core.async.logging :refer :all]))

(defn- define-sequence-protocol
  "This function will generate a vector with 2 interactions to send and receive the greet message.
  Notice how send and receivers are defined separately in order to allow for sequencing of actions!"
  []
  (create-protocol [(-->> "greet" "alice" "bob")
                    (-->> "greet" "alice" "carol")]))

;setup infrastructure, generate channels and add monitor
(def infrastructure (generate-infrastructure (define-sequence-protocol)))
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
