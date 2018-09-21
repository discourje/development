(ns discourje.core-test
  (:require [clojure.test :refer :all]
            [discourje.core :refer :all])
  (:refer-clojure :exclude [send])
  (:import (discourje.core message participant)))

(deftest messageTest
  (let [m (message. "hello")]
    (is (= (str "hello") (:data m)))))

(defrecord testSource [message]
  source
  (send [message] (:data message)))

(defrecord testSink [message]
  sink
  (receive [message] (:data message)))

;defining a test channel implementing both overloads:
;sending the message and sending the length of the string as message
(defrecord testChannel [ch s r m]
  discourje.core.channel
  (transmit [ch s r m] (fn [me] (.length (:data me)))))
  ;(testTransmit [source sink message] (fn [m] (.length (:data m))))) ;anonymous function applying java .length to string

;setting up some test data
(def sender (participant. "sender"))
(def receiver (participant. "receiver"))
(def messageToSend (message. "First message is send!"))

;test channel
(def chan (->testChannel chan testSource, testSink, messageToSend))
;
(let [s (.s chan)
      r (.r chan)
      m (.m chan)]
    (transmit chan s r m))