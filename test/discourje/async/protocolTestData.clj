(ns discourje.async.protocolTestData
  (:require [clojure.test :refer :all]
            [discourje.core.async.async :refer :all]))

(deftest interactableTest
  (let [inter (-->> "1" "A" "B")]
    (is (= "1" (get-action inter)))
    (is (= "A" (get-sender inter)))
    (is (= "B" (get-receivers inter)))))

(defn testSingleParallelProtocol []
  (create-protocol [(-->> "1" "A" ["B" "C"])]))

(defn testDualProtocol []
  (create-protocol [(-->> "1" "A" "B")
                    (-->> "2" "B" "A")]))

(defn testTripleProtocol[]
  (create-protocol [
                    (-->> "1" "A" "B")
                    (-->> "2" "B" "A")
                    (-->> "3" "A" "C")]))

(defn testParallelProtocol[]
  (create-protocol [
                    (-->> "1" "A" "B")
                    (-->> "2" "B" "A")
                    (-->> "3" "A" "C")
                    (-->> "4" "C" ["A" "B"])]))

(defn testQuadProtocol[]
  (create-protocol [
                    (-->> "start" "main" ["A" "B" "C"])
                    (-->> "1" "A" "B")
                    (-->> "2" "B" "A")
                    (-->> "3" "A" "C")
                    (-->> "4" "C" ["A" "B"])]))

