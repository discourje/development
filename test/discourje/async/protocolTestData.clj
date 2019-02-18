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

(defn testTripleProtocol []
  (create-protocol [
                    (-->> "1" "A" "B")
                    (-->> "2" "B" "A")
                    (-->> "3" "A" "C")]))

(defn testParallelProtocol []
  (create-protocol [
                    (-->> "1" "A" "B")
                    (-->> "2" "B" "A")
                    (-->> "3" "A" "C")
                    (-->> "4" "C" ["A" "B"])]))

(defn testQuadProtocol []
  (create-protocol [
                    (-->> "start" "main" ["A" "B" "C"])
                    (-->> "1" "A" "B")
                    (-->> "2" "B" "A")
                    (-->> "3" "A" "C")
                    (-->> "4" "C" ["A" "B"])]))

(defn single-choice-protocol []
  (create-protocol [(branch-on [
                                [(-->> "1" "A" "B")]
                                [(-->> "hi" "A" "C")]]
                               )]))

(defn single-choice-5branches-protocol []
  (create-protocol [(branch-on [
                                [(-->> "1" "A" "B")]
                                [(-->> "1" "A" "C")]
                                [(-->> "1" "A" "D")]
                                [(-->> "1" "A" "E")]
                                ]
                               )]))

(defn dual-choice-protocol []
  (create-protocol [(branch-on [
                                [(-->> "1" "A" "B")]
                                [(-->> "hi" "A" "C")
                                 (branch-on [
                                             [(-->> "hiA" "C" "A")]
                                             [(-->> "hiD" "C" "D")]]
                                            )]]
                               )]))

(defn single-nested-choice-branch-protocol []
  (create-protocol [(branch-on [
                                [(-->> "1" "A" "B")]
                                [(branch-on [
                                             [(-->> "1" "A" "C")]
                                             [(-->> "1" "A" "D")]]
                                            )]]
                               )]))

(defn multiple-nested-branches-protocol []
  (create-protocol [(branch-on [
                                [(branch-on [
                                             [(-->> "1" "A" "B")]
                                             [(-->> "1" "A" "C")]]
                                            )]
                                [(branch-on [
                                             [(branch-on [
                                                          [(branch-on [
                                                                       [(-->> "1" "A" "D")]
                                                                       [(-->> "1" "A" ["E" "F" "G"])]]
                                                                      )]
                                                          [(-->> "1" "A" "H")]]
                                                         )]
                                             [(-->> "1" "A" "I")]]
                                            )]]
                               )]))