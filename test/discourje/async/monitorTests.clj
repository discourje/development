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
  (let [mon (generate-monitor (testParallelProtocol false))]
    (is (= (:interactions mon) testParallelProtocolControl))))

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
    (is (= (:interactions mon) multiple-nested-branches-protocolControl))
    ))

(deftest single-recur-protocol-ids-test
  (let [mon (generate-monitor (single-recur-protocol false))]
    (is (= (:interactions mon) single-recur-protocolControl))))

(deftest nested-recur-protocol-ids-test
  (let [mon (generate-monitor (nested-recur-protocol false))]
    (is (= (:interactions mon) nested-recur-protocolControl))))

(deftest one-recur-with-choice-protocol-monitor-test
  (let [mon (generate-monitor (one-recur-with-choice-protocol))]
    (is (= 1 (count (:interactions mon))))))

(deftest one-recur-with-choice-protocol-ids-test
  (let [mon (generate-monitor (one-recur-with-choice-protocol))
        i0 (nth (:interactions mon) 0)
        i0r0 (nth (:recursion i0) 0)
        i0r0b00 (nth (nth (:branches i0r0) 0) 0)
        i0r0b01 (nth (nth (:branches i0r0) 0) 1)
        i0r0b10 (nth (nth (:branches i0r0) 1) 0)
        i0r0b11 (nth (nth (:branches i0r0) 1) 1)
        ]
    (is (= (get-next i0) nil))
    (is (= (get-next i0r0) nil))
    (is (= (get-next i0r0b00) (get-id i0r0b01)))
    (is (= (get-next i0r0b01) (get-id i0)))
    (is (= (get-next i0r0b10) (get-id i0r0b11)))
    (is (= (get-next i0r0b11) nil))))

(deftest one-recur-with-startchoice-and-endchoice-protocol-monitor-test
  (let [mon (generate-monitor (one-recur-with-startchoice-and-endchoice-protocol))]
    (is (= 1 (count (:interactions mon))))))

(deftest one-recur-with-startchoice-and-endchoice-protocol-ids-test
  (let [mon (generate-monitor (one-recur-with-startchoice-and-endchoice-protocol))
        i0 (nth (:interactions mon) 0)
        i0b0r0 (nth (nth (:branches i0) 0) 0)
        i0b0r00 (nth (:recursion i0b0r0) 0)
        i0b0r00b00 (nth (nth (:branches i0b0r00) 0) 0)
        i0b0r00b01 (nth (nth (:branches i0b0r00) 0) 1)
        i0b0r00b10 (nth (nth (:branches i0b0r00) 1) 0)
        i0b0r00b11 (nth (nth (:branches i0b0r00) 1) 1)
        i0b10 (nth (nth (:branches i0) 1) 0)
        ]
    (is (= (get-next i0) nil))
    (is (= (get-next i0b0r0) nil))
    (is (= (get-next i0b10) nil))
    (is (= (get-next i0b0r00) nil))
    (is (= (get-next i0b0r00b00) (get-id i0b0r00b01)))
    (is (= (get-next i0b0r00b01) (get-id i0b0r0)))
    (is (= (get-next i0b0r00b10) (get-id i0b0r00b11)))
    (is (= (get-next i0b0r00b11) nil))))

(deftest two-buyer-protocol-monitor-test
  (let [mon (generate-monitor (two-buyer-protocol))]
    (is (= 1 (count (:interactions mon))))))

(deftest two-buyer-protocol-ids-test
  (let [mon (generate-monitor (two-buyer-protocol))
        i0 (nth (:interactions mon) 0)
        i0r0i0 (nth (:recursion i0) 0)
        i0r0i1 (nth (:recursion i0) 1)
        i0r0i2 (nth (:recursion i0) 2)
        i0r0i3 (nth (:recursion i0) 3)
        i0r0i3b00 (nth (nth (:branches i0r0i3) 0) 0)
        i0r0i3b01 (nth (nth (:branches i0r0i3) 0) 1)
        i0r0i3b02 (nth (nth (:branches i0r0i3) 0) 2)
        i0r0i3b10 (nth (nth (:branches i0r0i3) 1) 0)
        i0r0i3b11 (nth (nth (:branches i0r0i3) 1) 1)
        ]
    (is (= (get-next i0) nil))
    (is (= (get-next i0r0i0) (get-id i0r0i1)))
    (is (= (get-next i0r0i1) (get-id i0r0i2)))
    (is (= (get-next i0r0i2) (get-id i0r0i3)))
    (is (= (get-next i0r0i3) nil))
    (is (= (get-next i0r0i3b00) (get-id i0r0i3b01)))
    (is (= (get-next i0r0i3b01) (get-id i0r0i3b02)))
    (is (= (get-next i0r0i3b02) (get-id i0)))
    (is (= (get-next i0r0i3b10) (get-id i0r0i3b11)))
    (is (= (get-next i0r0i3b11) nil))
    ))

(deftest apply-atomic-test
  (let [mon (generate-monitor (testDualProtocol))
        message (->message "1" "hello world")]
    (apply-interaction mon "A" "B" (get-label message))
    (is (= "2" (get-action (get-active-interaction mon))))
    (is (= "B" (get-sender (get-active-interaction mon))))
    (is (= "A" (get-receivers (get-active-interaction mon))))))