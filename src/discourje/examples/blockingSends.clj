(ns discourje.examples.blockingSends
  (require [discourje.api.api :refer :all]))

(defn- defineSequenceProtocol
  "This function will generate a vector with 4 monitors to send and receive the greet message.
  Notice that the monitors below send both greets from alice to bob.
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

(defn- greetForAlice
  "This function will use the protocol to send the greet message to bob."
  [participant]
  (s!!> "greet" (helperFunction 0) participant "bob"
        (s! "greet2" (helperFunction 1) participant "bob")))

(defn- greetForBob
  "This function will use the protocol to listen for the greet message."
  [participant]
  (r! "greet" "alice" participant (>r!> "greet2" "alice" participant (fn [x] (log (format "greet%s %s" x "Done!"))))))

;start the `greetForAlice' function on thread and add `alice' participant
(clojure.core.async/thread (greetForAlice alice))
;start the `greetForBob' function on thread and add `bob' participant
(clojure.core.async/thread (greetForBob bob))

;execute the following macroexpand-all to view generated code
(clojure.walk/macroexpand-all `(s!!> "greet" (helperFunction 0) alice "bob"
                                     (s! "greet2" (helperFunction 1) alice "bob")))

(clojure.walk/macroexpand-all `(r! "greet" "alice" bob
                                   (>r!> "greet2" "alice" bob helperFunction)))
