(ns discourje.core.async.examples.parallelisation
  (require [discourje.core.async.async :refer :all]
           [discourje.core.async.logging :refer :all]))

; This function will generate a vector with 1 interaction to send and receive the greet message.
;  Notice how receivers are defined as a vector in order to allow for parallelisation!
(def message-exchange-pattern
  (mep (-->> "greet" "alice" ["bob" "carol"])))

;setup infrastructure, generate channels and add monitor
(def infrastructure (add-infrastructure message-exchange-pattern))
;Get the channels
(def alice-to-bob (get-channel "alice" "bob" infrastructure))
(def alice-to-carol (get-channel "alice" "carol" infrastructure))

(defn- greet-bob-and-carol
  "This function will use the protocol to send the greet message to bob and carol.
 Notice: We supply the put operation with a vector of channels"
  []
  (>!! [alice-to-bob alice-to-carol] (msg "greet" "Greetings, from alice!")))

(defn- receive-greet
  "This function will use the protocol to listen for the greet message."
  [channel]
  (let [message (<!! channel "greet")]
    (log-message (format "Received message: %s by %s" (get-content message) (get-consumer channel)))))

;start the `greet-bob-and-carol' function on thread
(clojure.core.async/thread (greet-bob-and-carol))
;start the `receive-greet' function on thread and add the channel
(clojure.core.async/thread (receive-greet alice-to-bob))
;start the `receive-greet' function on thread and add the channel
(clojure.core.async/thread (receive-greet alice-to-carol))
