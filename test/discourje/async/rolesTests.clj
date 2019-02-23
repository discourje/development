(ns discourje.async.rolesTests
  (:require [clojure.test :refer :all]
            [discourje.async.protocolTestData :refer :all]
            [discourje.core.async.async :refer :all]))

(deftest unique2-roles-test
  (is (= 2 (count (get-distinct-roles (get-interactions (testDualProtocol)))))))

(deftest unique3-roles-test
  (is (= 3 (count (get-distinct-roles (get-interactions (testTripleProtocol)))))))

(deftest unique3-parallel-roles-test
  (is (= 3 (count (get-distinct-roles (get-interactions (testParallelProtocol)))))))

(deftest unique4-parallel-roles-test
  (is (= 4 (count (get-distinct-roles (get-interactions (testQuadProtocol)))))))

(deftest unique3-roles-single-choice-test
  (is (= 3 (count (get-distinct-roles (get-interactions (single-choice-protocol)))))))

(deftest unique7-roles-single-choice-5branches-test
  (is (= 7 (count (get-distinct-roles (get-interactions (single-choice-5branches-protocol)))))))

(deftest unique5-roles-dual-choice-test
  (is (= 5 (count (get-distinct-roles (get-interactions (dual-choice-protocol)))))))

(deftest unique4-roles-single-choice-multiple-interactions-protocol-test
  (is (= 4 (count (get-distinct-roles (get-interactions (single-choice-multiple-interactions-protocol)))))))

(deftest unique4-roles-single-nested-branch-choice-test
  (is (= 5 (count (get-distinct-roles (get-interactions (single-nested-choice-branch-protocol)))))))

(deftest unique10-roles-multiple-nested-branch-choice-test
  (is (= 10 (count (get-distinct-roles (get-interactions (multiple-nested-branches-protocol)))))))
