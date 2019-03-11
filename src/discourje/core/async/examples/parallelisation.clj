(ns discourje.core.async.examples.parallelisation
  (require [discourje.core.async.async :refer :all]
           [discourje.core.async.logging :refer :all]))

(defn- defineParallelProtocol
  "This function will generate a vector with 1 monitor to send and receive the greet message.
  Notice how receivers are defined as a vector in order to allow for parallelisation!"
  []
  (create-protocol [(-->> "greet" "alice" ["bob" "carol"])]))

;setup infrastructure, generate channels and add monitor
(def infrastructure (generate-infrastructure (defineParallelProtocol)))
;Get the channels
(def alice-to-bob (get-channel "alice" "bob" infrastructure))
(def alice-to-carol (get-channel "alice" "carol" infrastructure))

(defn- greetBobAndCarol
  "This function will use the protocol to send the greet message to bob and carol.
 Notice: We supply the put operation with a vector of channels"
  []
  (>!!! [alice-to-bob alice-to-carol] (->message "greet" "Greetings, from alice!")))

(defn- receiveGreet
  "This function will use the protocol to listen for the greet message."
  [channel]
  (let [message (<!!! channel "greet")]
    (log-message (format "Received message: %s by %s" (get-content message) (get-consumer channel)))))

;start the `GreetBobAndCarol' function on thread
(clojure.core.async/thread (greetBobAndCarol))
;start the `receiveGreet' function on thread and add the channel
(clojure.core.async/thread (receiveGreet alice-to-bob))
;start the `receiveGreet' function on thread and add the channel
(clojure.core.async/thread (receiveGreet alice-to-carol))
