(ns discourje.examples.customChannels
  (:require [discourje.core.async :refer :all]
           [discourje.core.logging :refer :all]))

; This function will generate a mep with 4 interactions to send and receive the greet message.
;  Notice how send and receivers are defined separately in order to allow for sequencing of actions!
(def message-exchange-pattern
  (mep (-->> String "alice" "bob")
       (-->> String "bob" "alice")
       (-->> String "alice" "carol")
       (-->> String "carol" "alice")
       (close "alice" "bob")
       (close "bob" "alice")
       (close "alice" "carol")
       (close "carol" "alice")))

;Define custom channels, which differ in buffer size (1 and 2, 3, 4)
(def a->b (chan "alice" "bob" 1))
(def b->a (chan "bob" "alice" 2))
(def a->c (chan "alice" "carol" 3))
(def c->a (chan "carol" "alice" 4))

;setup infrastructure, generate channels and add monitor, notice that we supply the generate-infrastructure function with custom channels vector
;generate-infrastructure will detect if all channels in the vector implement the transportable defprotocol and that all channels required for the protocol are present in the custom channel vector
(def infrastructure (add-infrastructure message-exchange-pattern [a->b b->a a->c c->a]))
;Get the channels
(def alice-to-bob (get-channel infrastructure "alice" "bob"))
(def bob-to-alice (get-channel infrastructure "bob" "alice"))
(def alice-to-carol (get-channel infrastructure "alice" "carol"))
(def carol-to-alice (get-channel infrastructure "carol" "alice"))

(defn- close-all
  "This function will use the protocol to listen for the greet message."
  []
  (do (close! alice-to-bob)
      (close! bob-to-alice)
      (close! alice-to-carol)
      (close! carol-to-alice)))

(defn- greet-bob-and-carol
  "This function will use the protocol to send the greet message to bob and carol."
  []
  (do (>!! alice-to-bob "Greetings, from alice!")
      (log-message (<!! bob-to-alice))
      (>!! alice-to-carol "Greetings, from alice!")
      (log-message (<!! carol-to-alice))
      (close-all)))

(defn- receive-greet
  "This function will use the protocol to listen for the greet message."
  [input-channel output-channel]
  (let [message (<!! input-channel)]
    (log-message (format "Received message: %s to %s" message (get-consumer input-channel)))
    (>!! output-channel "Hi to you too!")))

;start the `greet-bob-and-carol' function on thread
(clojure.core.async/thread (greet-bob-and-carol))
;start the `receive-greet' function on thread and add the channels
(clojure.core.async/thread (receive-greet alice-to-bob bob-to-alice))
;start the `receive-greet' function on thread and add the channels
(clojure.core.async/thread (receive-greet alice-to-carol carol-to-alice))