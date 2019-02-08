(ns discourje.async.protocolTestData
  (:require [clojure.test :refer :all]
            [discourje.core.async.async :refer :all]))

(deftest interactableTest
  (let [inter (-> "1" "A" "B")]
    (is (= "1" (get-action inter)))
    (is (= "A" (get-sender inter)))
    (is (= "B" (get-receiver inter)))))

(def testDualProtocol
  (create-protocol [
                    (-> "1" "A" "B")
                    (-> "2" "B" "A")]))

(def testTripleProtocol
  (create-protocol [
                    (-> "1" "A" "B")
                    (-> "2" "B" "A")
                    (-> "3" "A" "C")]))


(def testParallelProtocol
  (create-protocol [
                    (-> "1" "A" "B")
                    (-> "2" "B" "A")
                    (-> "3" "A" "C")
                    (-> "4" "C" ["A" "B"])]))


