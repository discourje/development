(ns discourje.examples.macroChaining
  (require [discourje.api.api :refer :all]))

(defn- defineSequenceProtocol
  "This function will generate a vector with 8 monitors to send and receive the greet message back and forth."
  []
  [(monitor-send "greet" "alice" "bob")
   (monitor-receive "greet" "bob" "alice")

   (monitor-send "greet2" "bob" "alice")
   (monitor-receive "greet2" "alice" "bob")

   (monitor-send "greet3" "alice" "bob")
   (monitor-receive "greet3" "bob" "alice")

   (monitor-send "greet4" "bob" "alice")
   (monitor-receive "greet4" "alice" "bob")
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
  (println (format "greet%s" x))
  (+ x 1))

(defn- greetForAlice
  "This function will use the protocol to send the greet message to bob and carol."
  [participant]
  (s!> "greet" 1 participant "bob"
       (r! "greet2" "bob" participant
           (>s!> "greet3" helperFunction participant "bob"
                 (r! "greet4" "bob" participant helperFunction)))))


(defn- greetForBob
  "This function will use the protocol to listen for the greet message."
  [participant]
  (r! "greet" "alice" participant
      (>s!> "greet2" helperFunction participant "alice"
            (r! "greet3" "alice" participant
                (>s! "greet4" helperFunction participant "alice")))))

;start the `GreetBobAndCarol' function on thread and add `alice' participant
(clojure.core.async/thread (greetForAlice alice))
;start the `receiveGreet' function on thread and add `bob' participant
(clojure.core.async/thread (greetForBob bob))

;execute the following macroexpand-all to view generated code
(clojure.walk/macroexpand-all `(s!> "greet" 1 alice "bob"
                                    (r! "greet2" "bob" alice
                                        (>s!> "greet3" helperFunction alice "bob"
                                              (r! "greet4" "bob" alice helperFunction)))))

(clojure.walk/macroexpand-all `(r! "greet" "alice" bob
                                   (>s!> "greet2" helperFunction bob "alice"
                                         (r! "greet3" "alice" bob
                                             (>s! "greet4" helperFunction bob "alice")))))