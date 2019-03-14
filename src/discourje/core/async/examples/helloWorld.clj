(ns discourje.core.async.examples.helloWorld
  (require [discourje.core.async.async :refer :all]
           [discourje.core.async.logging :refer :all]))

(defn- define-hello-world-protocol
  "This function will generate a vector with 1 monitor to send and receive the hello world message."
  []
  (create-protocol[(-->> "helloWorld" "user" "world")]))

;setup infrastructure, generate channels and add monitor
(def infrastructure (generate-infrastructure (define-hello-world-protocol)))
;define the participants
(def user-to-world (get-channel "user" "world" infrastructure))

(defn- send-to-world "This function will use the protocol to send the Hello World! message to world."
  [](>!! user-to-world (->message "helloWorld" "Hello World!")))

(defn- receive-from-user "This function will use the protocol to listen for the helloWorld message."
  [] (let [message (<!! user-to-world "helloWorld")]
    (log-message "World received message: " (get-content message))))

;start the `sendToWorld' function on thread
(clojure.core.async/thread (send-to-world))
;start the `receiveFromUser' function on thread
(clojure.core.async/thread (receive-from-user))