(ns discourje.examples.helloWorld
  (require [discourje.core.monitor :refer :all]
           [discourje.core.core :refer :all]
           [discourje.core.protocol :refer :all]
           [discourje.core.dataStructures :refer :all]))

(defn- defineHelloWorldProtocol
  "This function will generate a vector with 2 monitors to send and receive the hello world message."
  []
  (vector
    (->sendM "helloWorld" "user" "world")
    (->receiveM "helloWorld" "world" "user")))

(defn generateHelloWorldProtocol
  "Generate the protocol, channels and set the first monitor active."
  []
  (generateProtocol ["user" "world"] (defineHelloWorldProtocol)))

;define the protocol
(def protocol (atom (generateHelloWorldProtocol)))
;define the participants
(def user (discourje.core.core/->participant "user" protocol))
(def world (discourje.core.core/->participant "world" protocol))

(defn- sendToWorld
  "This function will use the protocol to send the Hello World! message to world."
  [participant]
  (println "Will now send Hello World! to world.")
  (send-to participant "helloWorld" "Hello World!" "world"))

(defn- receiveFromUser
  "This function will use the protocol to listen for the helloWorld message."
  [participant]
  (receive-by participant "helloWorld" "user"
              (fn [message]
                  (println (format "Received message: %s" message)))))

;start the `sendToWorld' function on thread and add `user' participant
(clojure.core.async/thread (sendToWorld user))
;start the `receiveFromUser' function on thread and add `world' participant
(clojure.core.async/thread (receiveFromUser world))