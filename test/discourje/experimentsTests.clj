(ns discourje.experimentsTests
  (:require [clojure.test :refer :all]
            [discourje.experiments.experiments :refer :all])
  (:refer-clojure :exclude [send])
  (:import (discourje.experiments.experiments channel message)))

(def testMessage (message. "i am sending"))
(def testChannel (channel. nil nil testMessage))
(def testSender (->sender "tester" testChannel))

(deftest messageTest
  (is (= (str "i am sending") (:data testMessage))))

(deftest channelTest
  (is (= (str "i am sending") (:data (.message testChannel)))))

(deftest nameTest
  (is (= (str "tester") (.name testSender))))