(ns discourje.async.monitorTests
  (:require [clojure.test :refer :all]
            [discourje.async.protocolTestData :refer :all]
            [discourje.core.async.async :refer :all]))

(deftest get-active-interaction-test
  (let [mon (generate-monitor testDualProtocol)]
    (is (= "1" (get-action (get-active-interaction mon))))
    (is (= "A" (get-sender (get-active-interaction mon))))
    (is (= "B" (get-receivers (get-active-interaction mon))))))

(deftest dual-protocol-monitor-test
  (let [mon (generate-monitor testDualProtocol)]
    (is (= 2 (count (:interactions mon))))
    (is (= 2 (count (:channels mon))))))

(deftest triple-protocol-monitor-test
  (let [mon (generate-monitor testTripleProtocol)]
    (is (= 3 (count (:interactions mon))))
    (is (= 6 (count (:channels mon))))))

(deftest parallel-protocol-monitor-test
  (let [mon (generate-monitor testParallelProtocol)]
    (is (= 4 (count (:interactions mon))))
    (is (= 6 (count (:channels mon))))))

(deftest quad-protocol-monitor-test
  (let [mon (generate-monitor testQuadProtocol)]
    (is (= 5 (count (:interactions mon))))
    (is (= 12 (count (:channels mon))))))
