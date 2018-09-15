(ns discourje.core-test
  (:require [clojure.test :refer :all]
            [discourje.core :refer :all])
  (:refer-clojure :exclude [send])
  (:import (discourje.core channel message)))

(def testMessage (message. "i am sending"))
(def testChannel (channel. nil nil testMessage))

(deftest messageTest
  (is (= (str "i am sending") (:data testMessage))))


(deftest channelTest
  (is (= (str "i am sending") (:data (.message testChannel)))))

;(:data (.bericht testChannel))




;;(def testSender
;;(-> sender "tester"
;;    (-> channel nil nil
;;        (-> message "i am sanding..."))))

;;(.verzend testSender)
