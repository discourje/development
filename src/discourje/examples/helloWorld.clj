(ns discourje.examples.helloWorld
  (require [discourje.api.api :refer :all]))

(defn- defineHelloWorldProtocol
  "This function will generate a vector with 2 monitors to send and receive the hello world message."
  []
  [(monitor-send "helloWorld" "user" "world")
    (monitor-receive "helloWorld" "world" "user")])

;define the protocol
(def protocol (generateProtocolFromMonitors (defineHelloWorldProtocol)))
;define the participants
(def user (generateParticipant "user" protocol))
(def world (generateParticipant "world" protocol))

(defn- sendToWorld
  "This function will use the protocol to send the Hello World! message to world."
  [participant]
  (println "Will now send Hello World! to world.")
  (s! "helloWorld" "Hello World!" participant "world"))

(defn- receiveFromUser
  "This function will use the protocol to listen for the helloWorld message."
  [participant]
  (r! "helloWorld" "user" participant
              (fn [message]
                  (println (format "Received message: %s" message)))))

;start the `sendToWorld' function on thread and add `user' participant
(clojure.core.async/thread (sendToWorld user))
;start the `receiveFromUser' function on thread and add `world' participant
(clojure.core.async/thread (receiveFromUser world))