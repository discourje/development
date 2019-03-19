(ns discourje.core.async.examples.sequencing
  (require [discourje.core.async.async :refer :all]
           [discourje.core.async.logging :refer :all]))

;  This function will generate a vector with 4 interactions to send and receive the greet message.
;  Notice how send and receivers are defined separately in order to allow for sequencing of actions!
(def message-exchange-pattern
  (mep (-->> "greet" "alice" "bob")
       (-->> "greet" "bob" "alice")
       (-->> "greet" "alice" "carol")
       (-->> "greet" "carol" "alice")))

;setup infrastructure, generate channels and add monitor
(def infrastructure (add-infrastructure message-exchange-pattern))
;Get the channels
(def alice-to-bob (get-channel "alice" "bob" infrastructure))
(def bob-to-alice (get-channel "bob" "alice" infrastructure))
(def alice-to-carol (get-channel "alice" "carol" infrastructure))
(def carol-to-alice (get-channel "carol" "alice" infrastructure))

(defn- greet-bob-and-carol
  "This function will use the protocol to send the greet message to bob and carol."
  []
  (>!! alice-to-bob (msg "greet" "Greetings, from alice!"))
  (log-message (get-content (<!! bob-to-alice "greet")))
  (>!! alice-to-carol (->message "greet" "Greetings, from alice!"))
  (log-message (get-content (<!! carol-to-alice "greet"))))

(defn- receive-greet
  "This function will use the protocol to listen for the greet message."
  [input-channel output-channel]
  (let [message (<!! input-channel "greet")]
    (log-message (format "Received message: %s to %s" (get-content message) (get-consumer input-channel)))
    (>!! output-channel (msg "greet" "Hi to you too!"))))

;start the `greet-bob-and-carol' function on thread
(clojure.core.async/thread (greet-bob-and-carol))
;start the `receive-greet' function on thread and add the channels
(clojure.core.async/thread (receive-greet alice-to-bob bob-to-alice))
;start the `receive-greet' function on thread and add the channels
(clojure.core.async/thread (receive-greet alice-to-carol carol-to-alice))