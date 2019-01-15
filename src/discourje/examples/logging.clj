(ns discourje.examples.logging
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
  "This function will use the protocol to send the greet message to bob and carol.
  NOTICE: How we first send to carol instead of bob, which is incorrect since our protocol specifies bob should be greeted first!"
  [participant]
  (log (format "%s will now send greet." (:name participant)))
  (s! "greet" (format "Greetings, from %s!" (:name participant)) participant "carol")
  (s! "greet" (format "Greetings, from %s!" (:name participant)) participant "bob"))

(defn- receiveGreet
  "This function will use the protocol to listen for the greet message."
  [participant]
  (r! "greet" "alice" participant
              (fn [message]
                  (log (format "%s Received message: %s" (:name participant) message)))))

"***BY DEFAULT EXCEPTION LOGGING IS ENABLED!***"

" NOTICE: How we first send to carol instead of bob, which is incorrect since our protocol specifies bob should be greeted first!
Now setting log-level to logging, the protocol will block communication although it does not comply with the protocol"
(set-monitor-logging)

"Uncomment the line below to enable exception logging which will throw exceptions when communication does not comply with the protocol
When exception mode is enabled, communication will be blocked when invalid!"
;(set-monitor-exceptions)

;start the `GreetBobAndCarol' function on thread and add `alice' participant
(clojure.core.async/thread (greetBobAndCarol alice))
;start the `receiveGreet' function on thread and add `bob' participant
(clojure.core.async/thread (receiveGreet bob))
;start the `receiveGreet' function on thread and add `carol' participant
(clojure.core.async/thread (receiveGreet carol))
