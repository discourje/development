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

(deftest unique5-roles-single-choice-5branches-test
  (is (= 5 (count (get-distinct-roles (get-interactions (single-choice-5branches-protocol)))))))

(deftest unique4-roles-dual-choice-test
  (is (= 4 (count (get-distinct-roles (get-interactions (dual-choice-protocol)))))))

(deftest unique4-roles-single-nested-branch-choice-test
  (is (= 4 (count (get-distinct-roles (get-interactions (single-nested-choice-branch-protocol)))))))

(deftest unique9-roles-multiple-nested-branch-choice-test
  (is (= 9 (count (get-distinct-roles (get-interactions (multiple-nested-branches-protocol)))))))
