(ns discourje.examples.helloWorld
  (require [discourje.core.async :refer :all]
           [discourje.core.logging :refer :all]))
;This function will generate a mep with 1 interaction to send and receive the hello world message.
(def message-exchange-pattern
  (mep (-->> "helloWorld" "user" "world")
       (close "user" "world")))

;setup infrastructure, generate channels and add monitor
(def infrastructure (add-infrastructure message-exchange-pattern))
;get the channel
(def user-to-world (get-channel infrastructure "user" "world"))

(defn- send-to-world "This function will use the protocol to send the Hello World! message to world."
  [] (>!! user-to-world (msg "helloWorld" "Hello World!")))

(defn- receive-from-user "This function will use the protocol to listen for the helloWorld message."
  [] (let [message (<!! user-to-world "helloWorld")]
       (log-message "World received message: " message)
       (close! user-to-world)))

;start the `sendToWorld' function on thread
(clojure.core.async/thread (send-to-world))
;start the `receiveFromUser' function on thread
(clojure.core.async/thread (receive-from-user))