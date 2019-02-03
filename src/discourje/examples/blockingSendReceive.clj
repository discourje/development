(ns discourje.examples.blockingSendReceive
  (require [discourje.api.api :refer :all]
           [discourje.core.core :refer :all]))

(defn- defineSequenceProtocol
  []
  [
   (monitor-send "greet" "alice" "bob")
   (monitor-receive "greet" "bob" "alice")
   (monitor-send "greet2" "bob" "alice")
   (monitor-receive "greet2" "alice" "bob")
   ])

;define the protocol
(def protocol (generateProtocolFromMonitors (defineSequenceProtocol) 1))
;define the participants
(def alice (generateParticipant "alice" protocol))
(def bob (generateParticipant "bob" protocol))
;define a helper function to print and return value for chained macros
(defn helperFunction
  "prints greet<input> and return input + 1"
  [x]
  (log (format "greet%s" x))
  (+ x 1))

(defn done
  "protocol done"
  [x]
  (println "done sending greet messages"))

(defn- greetForAlice
  "This function will use the protocol to send the greet message to bob."
  [participant]
  (dcj-send!! "greet" 0 (:name participant) "bob" protocol)
  (dcj-recv!! "greet2" "bob" (:name participant) protocol helperFunction))


(defn- greetForBob
  "This function will use the protocol to listen for the greet message."
  [participant]
  (dcj-recv!! "greet" "alice" (:name participant) protocol helperFunction)
  (dcj-send!! "greet2" 1 (:name participant) "alice" protocol)
  )

;start the `greetForAlice' function on thread and add `alice' participant
(clojure.core.async/thread (greetForAlice alice))
;start the `greetForBob' function on thread and add `bob' participant
(clojure.core.async/thread (greetForBob bob))