(ns discourje.async.monitorTests
  (:require [clojure.test :refer :all]
            [discourje.async.protocolTestData :refer :all]
            [discourje.core.async.async :refer :all]))

(deftest get-active-interaction-test
  (let [mon (generate-monitor (testDualProtocol))
        i0 (nth (:interactions mon) 0)
        i1 (nth (:interactions mon) 1)]
    (is (= "1" (get-action (get-active-interaction mon))))
    (is (= "A" (get-sender (get-active-interaction mon))))
    (is (= "B" (get-receivers (get-active-interaction mon))))
    (is (= (get-next i0) (get-id i1)))
    (is (= (get-next i1) nil))))

(deftest dual-protocol-monitor-test
  (let [mon (generate-monitor (testDualProtocol))]
    (is (= 2 (count (:interactions mon))))))

(deftest dual-protocol-ids-test
  (let [mon (generate-monitor (testDualProtocol))
        i0 (nth (:interactions mon) 0)
        i1 (nth (:interactions mon) 1)]
    (is (= (get-next i0) (get-id i1)))
    (is (= (get-next i1) nil))))

(deftest triple-protocol-monitor-test
  (let [mon (generate-monitor (testTripleProtocol))]
    (is (= 3 (count (:interactions mon))))))

(deftest triple-protocol-ids-test
  (let [mon (generate-monitor (testTripleProtocol))
        i0 (nth (:interactions mon) 0)
        i1 (nth (:interactions mon) 1)
        i2 (nth (:interactions mon) 2)]
    (is (= (get-next i0) (get-id i1)))
    (is (= (get-next i1) (get-id i2)))
    (is (= (get-next i2) nil))))

(deftest parallel-protocol-monitor-test
  (let [mon (generate-monitor (testParallelProtocol))]
    (is (= 4 (count (:interactions mon))))))

(deftest parallel-protocol-ids-test
  (let [mon (generate-monitor (testParallelProtocol))
        i0 (nth (:interactions mon) 0)
        i1 (nth (:interactions mon) 1)
        i2 (nth (:interactions mon) 2)
        i3 (nth (:interactions mon) 3)]
    (is (= (get-next i0) (get-id i1)))
    (is (= (get-next i1) (get-id i2)))
    (is (= (get-next i2) (get-id i3)))
    (is (= (get-next i3) nil))))

(deftest quad-protocol-monitor-test
  (let [mon (generate-monitor (testQuadProtocol))]
    (is (= 5 (count (:interactions mon))))))

(deftest quad-protocol-ids-test
  (let [mon (generate-monitor (testQuadProtocol))
        i0 (nth (:interactions mon) 0)
        i1 (nth (:interactions mon) 1)
        i2 (nth (:interactions mon) 2)
        i3 (nth (:interactions mon) 3)
        i4 (nth (:interactions mon) 4)]
    (is (= (get-next i0) (get-id i1)))
    (is (= (get-next i1) (get-id i2)))
    (is (= (get-next i2) (get-id i3)))
    (is (= (get-next i3) (get-id i4)))
    (is (= (get-next i4) nil))))

(deftest quad-protocol-monitor-test
  (let [mon (generate-monitor (testQuadProtocol))]
    (is (= 5 (count (:interactions mon))))))

(deftest single-choice-protocol-test
  (let [mon (generate-monitor (single-choice-protocol))]
    (is (= 1 (count (:interactions mon))))))

(deftest single-choice-in-middle-protocol-test
  (let [mon (generate-monitor (single-choice-in-middle-protocol))]
    (println (:interactions mon))
    (is (= 3 (count (:interactions mon))))))

(deftest single-choice-in-middle-protocol-ids-test
  (let [mon (generate-monitor (single-choice-in-middle-protocol))
        i0 (nth (:interactions mon) 0)
        i1 (nth (:interactions mon) 1)
        branch0 (nth (:branches i1) 0)
        i100 (nth branch0 0)
        i101 (nth branch0 1)
        branch1 (nth (:branches i1) 1)
        i110 (nth branch1 0)
        i111 (nth branch1 1)
        i2 (nth (:interactions mon) 2)]
    (is (= (get-next i0) (get-id i1)))
    (is (= (get-next i1) (get-id i2)))
    (is (= (get-next i100) (get-id i101)))
    (is (= (get-next i101) (get-id i2)))
    (is (= (get-next i110) (get-id i111)))
    (is (= (get-next i111) (get-id i2)))
    (is (= (get-next i2) nil))))

(deftest single-choice-5branches-protocol-test
  (let [mon (generate-monitor (single-choice-5branches-protocol))]
    (is (= 2 (count (:interactions mon))))))

(deftest single-choice-5branches-protocol-ids-test
  (let [mon (generate-monitor (single-choice-5branches-protocol))
        i0 (nth (:interactions mon) 0)
        i0b0 (nth (nth (:branches i0) 0) 0)
        i0b1 (nth (nth (:branches i0) 1) 0)
        i0b2 (nth (nth (:branches i0) 2) 0)
        i0b3 (nth (nth (:branches i0) 3) 0)
        i0b4 (nth (nth (:branches i0) 4) 0)
        i1 (nth (:interactions mon) 1)]
    (is (= (get-next i0) (get-id i1)))
    (is (= (get-next i0b0) (get-id i1)))
    (is (= (get-next i0b1) (get-id i1)))
    (is (= (get-next i0b2) (get-id i1)))
    (is (= (get-next i0b3) (get-id i1)))
    (is (= (get-next i0b4) (get-id i1)))
    (is (= (get-next i1) nil))))

(deftest dual-choice-protocol-test
  (let [mon (generate-monitor (dual-choice-protocol))]
    (is (= 2 (count (:interactions mon))))))

(deftest dual-choice-protocol-ids-test
  (let [mon (generate-monitor (dual-choice-protocol))
        i0 (nth (:interactions mon) 0)
        i0b0 (nth (nth (:branches i0) 0) 0)
        i0b10 (nth (nth (:branches i0) 1) 0)
        i0b11 (nth (nth (:branches i0) 1) 1)
        i0b1b10 (nth (nth (:branches i0b11)0)0)
        i0b1b11 (nth (nth (:branches i0b11)1)0)
        i1 (nth (:interactions mon) 1)]
    (is (= (get-next i0) (get-id i1)))
    (is (= (get-next i0b0) (get-id i1)))
    (is (= (get-next i0b10) (get-id i0b11)))
    (is (= (get-next i0b1b10) (get-id i1)))
    (is (= (get-next i0b1b11) (get-id i1)))
    (is (= (get-next i1) nil))))

(deftest multiple-nested-branches-protocol-test
  (let [mon (generate-monitor (multiple-nested-branches-protocol))]
    (is (= 2 (count (:interactions mon))))))

(deftest multiple-nested-branches-protocol-ids-test
  (let [mon (generate-monitor (multiple-nested-branches-protocol))
        i0 (nth (:interactions mon) 0)
        i0b0 (nth (:branches i0)0)
        i0b0b00 (nth (nth (:branches (nth i0b0 0))0)0)
        i0b0b01 (nth (nth (:branches (nth i0b0 0))0)1)
        i0b0b10 (nth (nth (:branches (nth i0b0 0))1)0)
        i0b1  (nth (:branches i0) 1)
        i0b1b0 (nth (nth (:branches (nth i0b1 0))0)0)
        i0b1b0b0 (nth (:branches i0b1b0)0)
        i0b1b0b0b00 (nth (nth (:branches (nth i0b1b0b0 0))0)0)
        i0b1b0b0b10 (nth (nth (:branches (nth i0b1b0b0 0))1)0)
        i0b1b0b0b11 (nth (nth (:branches (nth i0b1b0b0 0))1)1)
        i0b1b0b0b12 (nth (nth (:branches (nth i0b1b0b0 0))1)2)

        i0b1b0b10 (nth (nth (:branches i0b1b0)1)0)
        i0b1b11 (nth (nth (:branches (nth i0b1 0))1)0)
        i1 (nth (:interactions mon) 1)
       ]
    (println i0b1b0b0b10)
    (is (= (get-next i0) (get-id i1)))
    (is (= (get-next i0b0b00) (get-id i0b0b01)))
    (is (= (get-next i0b0b01) (get-id i1)))
    (is (= (get-next i0b0b10) (get-id i1)))
    (is (= (get-next i0b1b11) (get-id i1)))
    (is (= (get-next i0b1b0b10) (get-id i1)))
    (is (= (get-next i0b1b0b0b00) (get-id i1)))
    (is (= (get-next i0b1b0b0b10) (get-id i0b1b0b0b11)))
    (is (= (get-next i0b1b0b0b11) (get-id i0b1b0b0b12)))
    (is (= (get-next i0b1b0b0b12) (get-id i1)))
    ))

(deftest apply-atomic-test
  (let [mon (generate-monitor (testDualProtocol))
        message (->message "1" "hello world")]
    (receive-interaction mon (get-label message) "B")
    (is (= "2" (get-action (get-active-interaction mon))))
    (is (= "B" (get-sender (get-active-interaction mon))))
    (is (= "A" (get-receivers (get-active-interaction mon))))))

