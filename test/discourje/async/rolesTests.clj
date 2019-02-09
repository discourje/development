(ns discourje.async.rolesTests
  (:require [clojure.test :refer :all]
            [discourje.async.protocolTestData :refer :all]
            [discourje.core.async.async :refer :all]
            ))

(deftest unique2-roles-test
  []
  (is (= 2 (count (get-distinct-roles (get-interactions testDualProtocol))))))

(deftest unique3-roles-test
  []
  (is (= 3 (count (get-distinct-roles (get-interactions testTripleProtocol))))))

(deftest unique3-parallel-roles-test
  []
  (is (= 3 (count (get-distinct-roles (get-interactions testParallelProtocol))))))

(deftest unique4-parallel-roles-test
  []
  (is (= 4 (count (get-distinct-roles (get-interactions testQuadProtocol))))))

(deftest get-interactions-for-a
  []
  (is (= 2 (count (get-interactions-by-role "A" testDualProtocol) ))))


