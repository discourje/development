(ns discourje.examples.parallelisation
  (require [discourje.api.api :refer :all]))

(defn- defineParallelProtocol
  "This function will generate a vector with 2 monitors to send and receive the greet message.
  Notice how receivers are defined as a vector in order to allow for parallelisation!"
  []
  [(monitor-send "greet" "alice" ["bob" "carol"])
   (monitor-receive "greet" ["bob" "carol"] "alice")])
;define the protocol
(def protocol (generateProtocolFromMonitors (defineParallelProtocol)))
(println (:channels @protocol))
;define the participants
(def alice (generateParticipant "alice" protocol))
(def bob (generateParticipant "bob" protocol))
(def carol (generateParticipant "carol" protocol))

(defn- greetBobAndCarol
  "This function will use the protocol to send the greet message to bob and carol."
  [participant]
  (log (format "%s will now send greet." (:name participant)))
  (s! "greet" (format "Greetings, from %s!" (:name participant)) participant ["bob" "carol"]))

(defn- receiveGreet
  "This function will use the protocol to listen for the greet message."
  [participant]
  (r! "greet" "alice" participant
              (fn [message]
                (log (format "%s Received message: %s" (:name participant) message)))))

;start the `GreetBobAndCarol' function on thread and add `alice' participant
(clojure.core.async/thread (greetBobAndCarol alice))
;start the `receiveGreet' function on thread and add `bob' participant
(clojure.core.async/thread (receiveGreet bob))
;start the `receiveGreet' function on thread and add `carol' participant
(clojure.core.async/thread (receiveGreet carol))
