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
    (is (= (:interactions mon) testDualProtocolControl))
    (is (= "1" (get-action (get-active-interaction mon))))
    (is (= "A" (get-sender (get-active-interaction mon))))
    (is (= "B" (get-receivers (get-active-interaction mon))))))

(deftest triple-protocol-ids-test
  (let [mon (generate-monitor (testTripleProtocol false))]
    (is (= (:interactions mon) testTripleProtocolControl))))

(deftest parallel-protocol-ids-test
  (let [mon (generate-monitor (testMulticastProtocol false))]
    (is (= (:interactions mon) testMulticastProtocolControl))))

(deftest quad-protocol-ids-test
  (let [mon (generate-monitor (testQuadProtocol false))]
    (is (= (:interactions mon) testQuadProtocolControl))))

(deftest single-choice-in-middle-protocol-ids-test
  (let [mon (generate-monitor (single-choice-in-middle-protocol false))]
    (is (= (:interactions mon) single-choice-in-middle-protocolControl))))

(deftest single-choice-5branches-protocol-ids-test
  (let [mon (generate-monitor (single-choice-5branches-protocol false))]
    (is (= (:interactions mon) single-choice-5branches-protocolControl))))

(deftest dual-choice-protocol-ids-test
  (let [mon (generate-monitor (dual-choice-protocol false))]
    (is (= (:interactions mon) dual-choice-protocolControl))))


(deftest multiple-nested-choice-branch-protocol-ids-test
  (let [mon (generate-monitor (multiple-nested-choice-branch-protocol false))]
    (is (= (:interactions mon) multiple-nested-choice-branch-protocolControl))))

(deftest single-choice-multiple-interactions-protocol-test
  (let [mon (generate-monitor (single-choice-multiple-interactions-protocol false))]
    (is (= (:interactions mon) single-choice-multiple-interactions-protocolControl))))

(deftest multiple-nested-branches-protocol-ids-test
  (let [mon (generate-monitor (multiple-nested-branches-protocol false))]
    (is (= (:interactions mon) multiple-nested-branches-protocolControl))))

(deftest single-recur-protocol-ids-test
  (let [mon (generate-monitor (single-recur-protocol false))]
    (is (= (:interactions mon) single-recur-protocolControl))))

(deftest nested-recur-protocol-ids-test
  (let [mon (generate-monitor (nested-recur-protocol false))]
    (is (= (:interactions mon) nested-recur-protocolControl))))

(deftest one-recur-with-choice-protocol-ids-test
  (let [mon (generate-monitor (one-recur-with-choice-protocol false))]
    (is (= (:interactions mon)one-recur-with-choice-protocolControl))))

(deftest one-recur-with-startchoice-and-endchoice-protocol-ids-test
  (let [mon (generate-monitor (one-recur-with-startchoice-and-endchoice-protocol false))]
    (is (= (:interactions mon) one-recur-with-startchoice-and-endchoice-protocolControl))))

(deftest two-buyer-protocol-ids-test
  (let [mon (generate-monitor (two-buyer-protocol false))]
    (is (= (:interactions mon)two-buyer-protocolControl))))

(deftest apply-atomic-test
  (let [mon (generate-monitor (testDualProtocol true))
        message (->message "1" "hello world")]
    (apply-send! mon "A" "B" message)
    (apply-receive! mon "A" "B" (get-label message))
    (is (= "2" (get-action (get-active-interaction mon))))
    (is (= "B" (get-sender (get-active-interaction mon))))
    (is (= "A" (get-receivers (get-active-interaction mon))))))

(deftest parallel-after-interaction-test
  (let [mon (generate-monitor (parallel-after-interaction false))]
    (is (= (:interactions mon) parallel-after-interactionControl))))

(deftest parallel-after-interaction-with-after-test
  (let [mon (generate-monitor (parallel-after-interaction-with-after false))]
    (is (= (:interactions mon) parallel-after-interaction-with-afterControl))))

(deftest parallel-after-choice-with-after-test
  (let [mon (generate-monitor (parallel-after-choice-with-after false))]
    (is (= (:interactions mon) parallel-after-choice-with-afterControl))))

(deftest parallel-after-choice-with-after-choice-test
  (let [mon (generate-monitor (parallel-after-choice-with-after-choice false))]
    (is (= (:interactions mon) parallel-after-choice-with-after-choiceControl))))

(deftest parallel-after-rec-with-after-test
  (let [mon (generate-monitor (parallel-after-rec-with-after false))]
    (is (= (:interactions mon) parallel-after-rec-with-afterControl))))

(deftest parallel-after-rec-with-after-rec-test
  (let [mon (generate-monitor (parallel-after-rec-with-after-rec false))]
    (is (= (:interactions mon) parallel-after-rec-with-after-recControl))))

(deftest nested-parallel-test
  (let [mon (generate-monitor (nested-parallel false))]
    (is (= (:interactions mon) nested-parallelControl))))

(deftest after-parallel-nested-parallel-test
  (let [mon (generate-monitor (after-parallel-nested-parallel false))]
    (is (= (:interactions mon) after-parallel-nested-parallelControl))))

(deftest rec-with-parallel-with-choice-multicast-and-close-test
  (let [mon (generate-monitor (rec-with-parallel-with-choice-multicast-and-close false))]
    (is (= (:interactions mon) rec-with-parallel-with-choice-multicast-and-closeControl))))