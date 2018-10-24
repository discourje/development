(ns discourje.coreTests
  (:require [clojure.test :refer :all]
            [clojure.core.async :as async :refer :all]
            [discourje.core :refer :all]))

(def testingChannel (chan))
(deftest takeTest
  (putMessage testingChannel "hello")
  (is (go (= "hello" (<! (takeMessage testingChannel))))))