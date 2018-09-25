(ns discourje.experiments.refs)

(defrecord message [data])
(->message "hello")

(defprotocol source
  "A participant identified as a Sender."
  (se [source message] "The message to send"))

(defprotocol sink
  "A participant identified as a Receiver."
  (re [sink message] "Receive a message"))

(defrecord participant [name]
  source
  (se [this message] (format "Message send from: %s, content:" (:name this)))
  sink
  (re [this message] (format "%s received a message, %s" (:name this) (:data message))))

(def alice (ref (->participant "alice")))
(def bob (ref (->participant "bob")))

(def channel (ref ""))

(defn sendMessageSimple
  [source sink message]
  (dosync
    (ref-set channel
                  (re @sink
                   (->message (format "%s %s" (se @source message) message))))))

(sendMessageSimple alice bob "Hi there bob")
(sendMessageSimple bob alice "Hi there too alice")
(sendMessageSimple alice bob "Hi there again")

(defn sendMessageArity
  ([source sink message]
   (sendMessageArity source sink message
                     (re @sink
                         (->message (format "%s %s" (se @source message) message)))))
  ([source sink message function]
   (dosync
     (ref-set channel function))))


(sendMessageArity alice bob "Hey bob, my name is Alice.")
(sendMessageArity bob alice "Hi Alice." (re @bob
                                            (->message
                                              (format "This message is received with the arity overload! %s %s" (se @alice "Hi Alice.") "Hi Alice."))))
(def amountOfMessages (ref 0))
(defn updateAmountMessages [x]
  (dosync
    (ref-set amountOfMessages (+ @amountOfMessages x))))

(add-watch channel :stateWatcher
           (fn [key ref old-state new-state]
             (prn "-- channel has changed --")
             (if (= old-state new-state)
               (prn "Old and New are identical with value: " new-state)
               (prn (format
                      "old state %s
                      new state %s" old-state new-state)))
             (prn (format "messages received on channel: %s",(updateAmountMessages 1)))))