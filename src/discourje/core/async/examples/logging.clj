(ns discourje.core.async.examples.logging
  (require [discourje.core.async.async :refer :all]
           [discourje.core.async.logging :refer :all]))

;This function will generate a mep with 4 monitors to send and receive the greet message.
(def message-exchange-pattern
  (mep (-->> "greet" "alice" "bob")
       (-->> "greet" "bob" "alice")
       (-->> "greet" "alice" "carol")
       (-->> "greet" "carol" "alice")))

;setup infrastructure, generate channels and add monitor
(def infrastructure (add-infrastructure message-exchange-pattern))
;Get the channels
(def alice-to-bob (get-channel "alice" "bob" infrastructure))
(def bob-to-alice (get-channel "bob" "alice" infrastructure))
(def alice-to-carol (get-channel "alice" "carol" infrastructure))
(def carol-to-alice (get-channel "carol" "alice" infrastructure))

(defn- greet-bob-and-carol
  "This function will use the protocol to send the greet message to bob and carol."
  []
  (>!! alice-to-carol (msg "greet" "Greetings, from alice!"))
  (log-message (get-content (<!! alice-to-carol "greet")))
  (>!! alice-to-bob (->message "greet" "Greetings, from alice!"))
  (log-message (get-content (<!! bob-to-alice "greet"))))

(defn- receive-greet
  "This function will use the protocol to listen for the greet message."
  [input-channel output-channel]
  (let [message (<!! input-channel "greet")]
    (log-message (format "Received message: %s to %s" (get-content message) (get-consumer input-channel)))
    (>!! output-channel (msg "greet" "Hi to you too!"))))

"***BY DEFAULT EXCEPTION LOGGING IS ENABLED!***"

" NOTICE: How we first send to carol instead of bob, which is incorrect since our protocol specifies bob should be greeted first!
Now setting log-level to logging, the protocol will NOT block communication although it does not comply with the protocol"
(set-logging)

"Uncomment the line below to enable exception throwing which will throw exceptions when communication does not comply with the protocol
When exception mode is enabled, communication will be blocked when invalid!"
;(set-logging-exceptions)

"Uncomment the line below to enable exception throwing and logging which will throw exceptions when communication does not comply with the protocol
When exception mode is enabled, communication will be blocked when invalid!"
;(set-logging-and-exceptions)

"You can also set logging to none, to show no logs or exceptions"
;(set-logging-none)

;start the `greet-bob-and-carol' function on thread
(clojure.core.async/thread (greet-bob-and-carol))
;start the `receive-greet' function on thread and add the channels
(clojure.core.async/thread (receive-greet alice-to-bob bob-to-alice))
;start the `receive-greet' function on thread and add the channels
(clojure.core.async/thread (receive-greet alice-to-carol carol-to-alice))
