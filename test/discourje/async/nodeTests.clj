(ns discourje.async.nodeTests
  (:require [clojure.test :refer :all]
            [discourje.core.async.async :refer :all]
            [discourje.async.protocolTestData :refer :all]))

;
;(deftest transitions-test-dual
;  (is (= 2 (count (get-transitions-in-protocol testDualProtocol)))))
;
;(deftest transitions-test-triple
;  (is (= 3 (count (get-transitions-in-protocol testTripleProtocol)))))
;
;(deftest transitions-test-parallel
;  (is (= 4 (count (get-transitions-in-protocol testParallelProtocol)))))
;
;(deftest transitions-test-quad
;  (is (= 5 (count (get-transitions-in-protocol testQuadProtocol)))))

;; A -1-> B
;; B -2-> A
;; A -3-> C
;; C -4-> A, B
;(deftest interactions-to-transitions-dual-test
;  (let [trans (get-transitions-in-protocol  testDualProtocol)]
;    (println trans)))
;
