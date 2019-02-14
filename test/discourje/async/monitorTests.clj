(ns discourje.async.monitorTests
  (:require [clojure.test :refer :all]
            [discourje.async.protocolTestData :refer :all]
            [discourje.core.async.async :refer :all]))

(deftest get-active-interaction-test
  (let [mon (generate-monitor testDualProtocol)
        i0 (nth (:interactions mon) 0)
        i1 (nth (:interactions mon) 1)]
    (is (= "1" (get-action (get-active-interaction mon))))
    (is (= "A" (get-sender (get-active-interaction mon))))
    (is (= "B" (get-receivers (get-active-interaction mon))))
    (is (= (get-next i0) (get-id i1)))
    (is (= (get-next i1) nil))))

(deftest dual-protocol-monitor-test
  (let [mon (generate-monitor testDualProtocol)]
    (is (= 2 (count (:interactions mon))))))

(deftest dual-protocol-ids-test
  (let [mon (generate-monitor testDualProtocol)
        i0 (nth (:interactions mon) 0)
        i1 (nth (:interactions mon) 1)]
  (is (= (get-next i0) (get-id i1)))
  (is (= (get-next i1) nil))))

(deftest triple-protocol-monitor-test
  (let [mon (generate-monitor testTripleProtocol)]
    (is (= 3 (count (:interactions mon))))))

(deftest triple-protocol-ids-test
  (let [mon (generate-monitor testTripleProtocol)
        i0 (nth (:interactions mon) 0)
        i1 (nth (:interactions mon) 1)
        i2 (nth (:interactions mon) 2)]
    (is (= (get-next i0) (get-id i1)))
    (is (= (get-next i1) (get-id i2)))
    (is (= (get-next i2) nil))))

(deftest parallel-protocol-monitor-test
  (let [mon (generate-monitor testParallelProtocol)]
    (is (= 4 (count (:interactions mon))))))

(deftest parallel-protocol-ids-test
  (let [mon (generate-monitor testParallelProtocol)
        i0 (nth (:interactions mon) 0)
        i1 (nth (:interactions mon) 1)
        i2 (nth (:interactions mon) 2)
        i3 (nth (:interactions mon) 3)]
    (is (= (get-next i0) (get-id i1)))
    (is (= (get-next i1) (get-id i2)))
    (is (= (get-next i2) (get-id i3)))
    (is (= (get-next i3) nil))))

(deftest quad-protocol-monitor-test
  (let [mon (generate-monitor testQuadProtocol)]
    (is (= 5 (count (:interactions mon))))))

(deftest quad-protocol-ids-test
  (let [mon (generate-monitor testQuadProtocol)
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