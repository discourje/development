(ns discourje.examples.sequencing
  (require [discourje.api.api :refer :all]))

(defn- defineSequenceProtocol
  "This function will generate a vector with 4 monitors to send and receive the greet message.
  Notice how send and receivers are defined separately in order to allow for sequencing of actions!"
  []
  [(monitor-send "greet" "alice" "bob")
    (monitor-send "greet" "alice" "carol")
    (monitor-receive "greet" "bob" "alice")
    (monitor-receive "greet" "carol" "alice")])

;define the protocol
(def protocol (generateProtocolFromMonitors (defineSequenceProtocol)))
;define the participants
(def alice (generateParticipant "alice" protocol))
(def bob (generateParticipant "bob" protocol))
(def carol (generateParticipant "carol" protocol))

(defn- greetBobAndCarol
  "This function will use the protocol to send the greet message to bob and carol."
  [participant]
  (println (format "%s will now send greet." (:name participant)))
  (s! "greet" (format "Greetings, from %s!" (:name participant)) participant "bob")
  (s! "greet" (format "Greetings, from %s!" (:name participant)) participant "carol"))

(defn- receiveGreet
  "This function will use the protocol to listen for the greet message."
  [participant]
  (r! "greet" "alice" participant
              (fn [message]
                  (println (format "%s Received message: %s" (:name participant) message)))))

;start the `GreetBobAndCarol' function on thread and add `alice' participant
(clojure.core.async/thread (greetBobAndCarol alice))
;start the `receiveGreet' function on thread and add `bob' participant
(clojure.core.async/thread (receiveGreet bob))
;start the `receiveGreet' function on thread and add `carol' participant
(clojure.core.async/thread (receiveGreet carol))
