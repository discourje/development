(ns discourje.async.monitorTests
  (:require [clojure.test :refer :all]
            [discourje.async.protocolTestData :refer :all]
            [discourje.core.async :refer :all])
  (:import (clojure.lang Atom)))

(defn nth-next [root index]
  (loop [node (if (and (not= nil root) (instance? Atom root)) @root root)
         value 0]
    (if (== value index)
      node
      (recur @(get-next node) (+ value 1)))))

(deftest get-active-interaction-test
  (let [mon (generate-monitor (testDualProtocol false))]
    (println mon)
    (is (= (get-active-interaction mon) testDualProtocolControl))
    (is (= "1" (get-action (get-active-interaction mon))))
    (is (= "A" (get-sender (get-active-interaction mon))))
    (is (= "B" (get-receivers (get-active-interaction mon))))))

(deftest triple-protocol-ids-test
  (let [mon (generate-monitor (testTripleProtocol false))]
    (is (= (get-active-interaction mon) testTripleProtocolControl))))

(deftest parallel-protocol-ids-test
  (let [mon (generate-monitor (testMulticastProtocol false))]
    (is (= (get-active-interaction mon) testMulticastProtocolControl))))

(deftest quad-protocol-ids-test
  (let [mon (generate-monitor (testQuadProtocol false))]
    (is (= (get-active-interaction mon) testQuadProtocolControl))))

(deftest single-choice-in-middle-protocol-ids-test
  (let [mon (generate-monitor (single-choice-in-middle-protocol false))]
    (is (= (get-active-interaction mon) single-choice-in-middle-protocolControl))))

(deftest single-choice-5branches-protocol-ids-test
  (let [mon (generate-monitor (single-choice-5branches-protocol false))]
    (is (= (get-active-interaction mon) single-choice-5branches-protocolControl))))

(deftest dual-choice-protocol-ids-test
  (let [mon (generate-monitor (dual-choice-protocol false))]
    (is (= (get-active-interaction mon) dual-choice-protocolControl))))

(deftest multiple-nested-choice-branch-protocol-ids-test
  (let [mon (generate-monitor (multiple-nested-choice-branch-protocol false))]
    (is (= (get-active-interaction mon) multiple-nested-choice-branch-protocolControl))))

(deftest single-choice-multiple-interactions-protocol-test
  (let [mon (generate-monitor (single-choice-multiple-interactions-protocol false))]
    (is (= (get-active-interaction mon) single-choice-multiple-interactions-protocolControl))))

(deftest multiple-nested-branches-protocol-ids-test
  (let [mon (generate-monitor (multiple-nested-branches-protocol false))]
    (is (= (get-active-interaction mon) multiple-nested-branches-protocolControl))))

(deftest single-recur-protocol-ids-test
  (let [mon (generate-monitor (single-recur-protocol false))]
    (println (:recursion-set mon))
    (is (= (get-active-interaction mon) single-recur-protocolControl))))

(deftest nested-recur-protocol-ids-test
  (let [mon (generate-monitor (nested-recur-protocol false))]
    (println (:recursion-set mon))
    (is (= (get-active-interaction mon) nested-recur-protocolControl))))

(deftest parallel-with-rec-protocol-ids-test
  (let [mon (generate-monitor (parallel-with-rec false))]
    (is (= (get-active-interaction mon) parallel-with-rec-control))
    (is (not-empty @(:recursion-set mon)))))

(deftest one-recur-with-choice-protocol-ids-test
  (let [mon (generate-monitor (one-recur-with-choice-protocol false))]
    (println (:recursion-set mon))
    (is (= (get-active-interaction mon)one-recur-with-choice-protocolControl))))

(deftest rec-with-parallel-with-choice-multicast-ids-test
  (let [mon (generate-monitor (rec-with-parallel-with-choice-multicast false))]
    (println(:recursion-set mon))
    (is (not-empty @(:recursion-set mon)))))
(deftest one-recur-with-startchoice-and-endchoice-protocol-ids-test
  (let [mon (generate-monitor (one-recur-with-startchoice-and-endchoice-protocol false))]
    (is (= (get-active-interaction mon) one-recur-with-startchoice-and-endchoice-protocolControl))))

(deftest two-buyer-protocol-ids-test
  (let [mon (generate-monitor (two-buyer-protocol false))]
    (is (= (get-active-interaction mon)two-buyer-protocolControl))))

(deftest parallel-after-interaction-test
  (let [mon (generate-monitor (parallel-after-interaction false))]
    (is (= (get-active-interaction mon) parallel-after-interactionControl))))

(deftest parallel-after-interaction-with-after-test
  (let [mon (generate-monitor (parallel-after-interaction-with-after false))]
    (is (= (get-active-interaction mon) parallel-after-interaction-with-afterControl))))

(deftest parallel-after-choice-with-after-test
  (let [mon (generate-monitor (parallel-after-choice-with-after false))]
    (is (= (get-active-interaction mon) parallel-after-choice-with-afterControl))))

(deftest parallel-after-choice-with-after-choice-test
  (let [mon (generate-monitor (parallel-after-choice-with-after-choice false))]
    (is (= (get-active-interaction mon) parallel-after-choice-with-after-choiceControl))))

(deftest parallel-after-rec-with-after-test
  (let [mon (generate-monitor (parallel-after-rec-with-after false))]
    (is (= (get-active-interaction mon) parallel-after-rec-with-afterControl))))

(deftest parallel-after-rec-with-after-rec-test
  (let [mon (generate-monitor (parallel-after-rec-with-after-rec false))]
    (is (= (get-active-interaction mon) parallel-after-rec-with-after-recControl))))

(deftest nested-parallel-test
  (let [mon (generate-monitor (nested-parallel false))]
    (is (= (get-active-interaction mon) nested-parallelControl))))

(deftest after-parallel-nested-parallel-test
  (let [mon (generate-monitor (after-parallel-nested-parallel false))]
    (is (= (get-active-interaction mon) after-parallel-nested-parallelControl))))

(deftest rec-with-parallel-with-choice-multicast-and-close-test
  (let [mon (generate-monitor (rec-with-parallel-with-choice-multicast-and-close false))]
    (is (not-empty @(:recursion-set mon)))
    (is (= (get-active-interaction mon) rec-with-parallel-with-choice-multicast-and-closeControl))))