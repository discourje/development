(ns discourje.core-test
  (:require [clojure.test :refer :all]
            [discourje.core :refer :all]))

(def testSender
  (->sender "tester"
            (->channel nil nil
                       (->message "i am sanding..."))))

(.verzend testSender)
