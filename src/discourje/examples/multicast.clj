(ns discourje.examples.multicast
  (require [discourje.core.async :refer :all]
           [discourje.core.logging :refer :all]))

; This function will generate a vector with 1 interaction to send and receive the greet message.
;  Notice how receivers are defined as a vector in order to allow for multicast!
(def message-exchange-pattern
  (mep (-->> String "alice" ["bob" "carol"])
       (-->> String "bob" "alice")
       (close "alice" "bob")
       (close "alice" "carol")
       (close "bob" "alice")))

;setup infrastructure, generate channels and add monitor
(def infrastructure (add-infrastructure message-exchange-pattern))
;Get the channels
(def alice-to-bob (get-channel infrastructure "alice" "bob"))
(def alice-to-carol (get-channel infrastructure "alice" "carol"))
(def bob-to-alice (get-channel infrastructure "bob" "alice"))

(defn- greet-bob-and-carol
  "This function will use the protocol to send the greet message to bob and carol.
 Notice: We supply the put operation with a vector of channels"
  []
  (>!! [alice-to-bob alice-to-carol] "Greetings, from alice!")
  (<!! bob-to-alice)
  (close! alice-to-bob)
  (close! alice-to-carol)
  (close! bob-to-alice))

(defn- receive-greet
  "This function will use the protocol to listen for the greet message."
  [channel return-channel]
  (let [message (<!!! channel)]
    (log-message (format "Received message: %s by %s" message (get-consumer channel)))
    (when (not= (nil? return-channel)) (>!! return-channel "Hi Alice!"))))

;start the `greet-bob-and-carol' function on thread
(clojure.core.async/thread (greet-bob-and-carol))
;start the `receive-greet' function on thread and add the channel
(clojure.core.async/thread (receive-greet alice-to-bob bob-to-alice))
;start the `receive-greet' function on thread and add the channel
(clojure.core.async/thread (receive-greet alice-to-carol nil))
