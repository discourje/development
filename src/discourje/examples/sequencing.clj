(ns discourje.examples.sequencing
  (require [discourje.core.monitor :refer :all]
           [discourje.core.core :refer :all]
           [discourje.core.protocol :refer :all]
           [discourje.core.dataStructures :refer :all]))

(defn- defineSequenceProtocol
  "This function will generate a vector with 4 monitors to send and receive the greet message.
  Notice how send and receivers are defined separately in order to allow for sequencing of actions!"
  []
  (vector
    (->sendM "greet" "alice" "bob")
    (->sendM "greet" "alice" "carol")
    (->receiveM "greet" "bob" "alice")
    (->receiveM "greet" "carol" "alice")))

(defn generateSequenceProtocol
  "Generate the protocol, channels and set the first monitor active."
  []
  (generateProtocol (defineSequenceProtocol)))

;define the protocol
(def protocol (atom (generateSequenceProtocol)))
;define the participants
(def alice (discourje.core.core/->participant "alice" protocol))
(def bob (discourje.core.core/->participant "bob" protocol))
(def carol (discourje.core.core/->participant "carol" protocol))

(defn- greetBobAndCarol
  "This function will use the protocol to send the greet message to bob and carol."
  [participant]
  (println (format "%s will now send greet." (:name participant)))
  (send-to participant "greet" (format "Greetings, from %s!" (:name participant)) "bob")
  (send-to participant "greet" (format "Greetings, from %s!" (:name participant)) "carol"))

(defn- receiveGreet
  "This function will use the protocol to listen for the greet message."
  [participant]
  (receive-by participant "greet" "alice"
              (fn [message]
                  (println (format "%s Received message: %s" (:name participant) message)))))

;start the `GreetBobAndCarol' function on thread and add `alice' participant
(clojure.core.async/thread (greetBobAndCarol alice))
;start the `receiveGreet' function on thread and add `bob' participant
(clojure.core.async/thread (receiveGreet bob))
;start the `receiveGreet' function on thread and add `carol' participant
(clojure.core.async/thread (receiveGreet carol))
