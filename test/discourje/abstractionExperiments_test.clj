(ns discourje.abstractionExperiments-test
  (:require [clojure.test :refer :all]
            [discourje.experiments.abstractionExperiments :refer :all])
  (:refer-clojure :exclude [send])
  (:import (discourje.experiments.abstractionExperiments message participant)))

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
  channel
  (transmit [ch s r m]
    (let [mes m]
      (str (:data mes)))))

;setting up some test data
(def sender (participant. "sender"))
(def receiver (participant. "receiver"))
(def messageToSend (message. "First message is send!"))

(fn [messageToSend] (str (:data messageToSend)))

(let [m messageToSend]
  (str (:data m)))

;test channel
(def chan (->testChannel chan sender, receiver, messageToSend))
;
(let [s (.s chan)
      r (.r chan)
      m (.m chan)]
    (transmit chan s r m))