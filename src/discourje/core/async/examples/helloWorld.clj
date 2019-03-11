(ns discourje.core.async.examples.helloWorld
  (require [discourje.core.async.async :refer :all]
           [discourje.core.async.logging :refer :all]))

(defn- defineHelloWorldProtocol
  "This function will generate a vector with 1 monitor to send and receive the hello world message."
  []
  (create-protocol[(-->> "helloWorld" "user" "world")]))

;setup infrastructure, generate channels and add monitor
(def infrastructure (generate-infrastructure (defineHelloWorldProtocol)))
;define the participants
(def user-to-world (get-channel "user" "world" infrastructure))

(defn- sendToWorld
  "This function will use the protocol to send the Hello World! message to world."
  []
  (>!!! user-to-world (->message "helloWorld" "Hello World!")))

(defn- receiveFromUser
  "This function will use the protocol to listen for the helloWorld message."
  []
  (let [message (<!!! user-to-world "helloWorld")]
    (log-message "World received message: " (get-content message))))

;start the `sendToWorld' function on thread
(clojure.core.async/thread (sendToWorld))
;start the `receiveFromUser' function on thread
(clojure.core.async/thread (receiveFromUser))