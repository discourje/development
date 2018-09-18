(ns discourje.core-test
  (:require [clojure.test :refer :all]
            [discourje.core :refer :all])
  (:refer-clojure :exclude [send])
  (:import (discourje.core message channel participant)))

(deftest messageTest
  (let [m (message. "hello")]
    (is (= (str "hello") (:data m)))))

;setting up some test data
(def sender (participant. "sender"))
(def receiver (participant. "receiver"))
(def messageToSend (message. "First message is send!"))

;defining a test channel implementing both overloads:
;sending the message and sending the length of the string as message
(def testChannel
    channel
  (transmit sender receiver messageToSend
            )
  (transmit sender receiver messageToSend (fn [m] (.length (:data m) )))) ;anonymous function applying java .length to string