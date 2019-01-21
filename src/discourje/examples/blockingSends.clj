(ns discourje.examples.blockingSends
  (require [discourje.api.api :refer :all]))

(defn- defineSequenceProtocol
  "This function will generate a vector with 4 monitors to send and receive the greet message back and forth.
  Notice that the monitors below send greet 5 and 6 from alice to bob.
  Since the protocol does not describe any way for bob to notify alice that the value is received you need to know when the value is taken.\n  To be able to add some synchronous send operations you can use a callback function for send.
  This callback will be invoked when a value is successfully taken, so you can now chain send functions.
  This simulates blocking behavior"
  []
  [
   (monitor-send "greet" "alice" "bob")
   (monitor-receive "greet" "bob" "alice")
   (monitor-send "greet2" "alice" "bob")
   (monitor-receive "greet2" "bob" "alice")
   ])

;define the protocol
(def protocol (generateProtocolFromMonitors (defineSequenceProtocol)))
;define the participants
(def alice (generateParticipant "alice" protocol))
(def bob (generateParticipant "bob" protocol))
;define a helper function to print and return value for chained macros
(defn helperFunction
  "prints greet<input> and return input + 1"
  [x]
  (log (format "greet%s" x))
  (+ x 1))

;(defn- greetForAlice
;  "This function will use the protocol to send the greet message to bob and carol."
;  [participant]
;  (s!> "greet" 1 participant "bob"
;       (r! "greet2" "bob" participant
;           (>s!> "greet3" helperFunction participant "bob"
;                 (r! "greet4" "bob" participant helperFunction)))))

(defn- greetForAlice
  "This function will use the protocol to send the greet message to bob and carol."
  [participant]
  (s!!> "greet" helperFunction participant "bob"
        (s! "greet2" helperFunction participant "bob")))


(defn- greetForBob
  "This function will use the protocol to listen for the greet message."
  [participant]
  (r! "greet" "alice" participant (fn [x] (r! "greet2" "alice" participant log))))

;start the `GreetBobAndCarol' function on thread and add `alice' participant
(clojure.core.async/thread (greetForAlice alice))
;start the `receiveGreet' function on thread and add `bob' participant
(clojure.core.async/thread (greetForBob bob))

;execute the following macroexpand-all to view generated code
(clojure.walk/macroexpand-all `(s!!> "greet" helperFunction alice "bob"
                                     (s! "greet2" helperFunction alice "bob")))
