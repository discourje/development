(ns discourje.async.protocolTests
  (:require [clojure.test :refer :all]
            [discourje.async.protocolTestData :refer :all]
            [discourje.core.async.async :refer :all]))

(deftest unique2-roles-test
  []
  (is (= 2 (count (getDistinctRoles (get-interactions testDualProtocol))))))

(deftest unique3-roles-test
  []
  (is (= 3 (count (getDistinctRoles (get-interactions testTripleProtocol))))))

(deftest unique3-parallel-roles-test
  []
  (is (= 3 (count (getDistinctRoles (get-interactions testParallelProtocol))))))


