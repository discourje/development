(ns discourje.examples.parallelisation
  (require [discourje.core.monitor :refer :all]
           [discourje.core.core :refer :all]
           [discourje.core.protocol :refer :all]
           [discourje.core.dataStructures :refer :all]))

(defn- defineParallelProtocol
  "This function will generate a vector with 2 monitors to send and receive the greet message.
  Notice how receivers are defined as a vector in order to allow for parallelisation!"
  []
  (vector
    (->sendM "greet" "alice" ["bob" "carol"])
    (->receiveM "greet" ["bob" "carol"] "alice")
    ))

(defn generateParallelProtocol
  "Generate the protocol, channels and set the first monitor active."
  []
  (generateProtocol ["alice" "bob" "carol"] (defineParallelProtocol)))

;define the protocol
(def protocol (atom (generateParallelProtocol)))
;define the participants
(def alice (discourje.core.core/->participant "alice" protocol))
(def bob (discourje.core.core/->participant "bob" protocol))
(def carol (discourje.core.core/->participant "carol" protocol))

(defn- GreetBobAndCarol
  "This function will use the protocol to send the greet message to bob and carol."
  [participant]
  (println (format "%s will now send greet." (:name participant)))
  (send-to participant "greet" (format "Greetings, from %s!" (:name participant)) ["bob" "carol"]))

(defn- receiveGreet
  "This function will use the protocol to listen for the greet message."
  [participant]
  (receive-by participant "greet" "alice"
              (fn [message]
                  (println (format "%s Received message: %s" (:name participant) message)))))

;start the `GreetBobAndCarol' function on thread and add `alice' participant
(clojure.core.async/thread (GreetBobAndCarol alice))
;start the `receiveGreet' function on thread and add `bob' participant
(clojure.core.async/thread (receiveGreet bob))
;start the `receiveGreet' function on thread and add `carol' participant
(clojure.core.async/thread (receiveGreet carol))
