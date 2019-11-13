(ns discourje.async.rolesTests
  (:require [clojure.test :refer :all]
            [discourje.async.protocolTestData :refer :all]
            [discourje.core.async :refer :all]))

(deftest unique2-roles-test
  (is (= 2 (count (get-distinct-role-pairs (get-interactions (testDualProtocol true)))))))

(deftest unique3-roles-test
  (is (= 3 (count (get-distinct-role-pairs (get-interactions (testTripleProtocol true)))))))

(deftest unique4-parallel-roles-test
  (is (= 4 (count (get-distinct-role-pairs (get-interactions (testMulticastProtocol true)))))))

(deftest unique5-parallel-roles-test
  (is (= 5 (count (get-distinct-role-pairs (get-interactions (testQuadProtocol true)))))))

(deftest unique2-roles-single-choice-test
  (is (= 2 (count (get-distinct-role-pairs (get-interactions (single-choice-protocol)))))))

(deftest unique6-roles-single-choice-5branches-test
  (is (= 6 (count (get-distinct-role-pairs (get-interactions (single-choice-5branches-protocol true)))))))

(deftest unique5-roles-dual-choice-test
  (is (= 5 (count (get-distinct-role-pairs (get-interactions (dual-choice-protocol true)))))))

(deftest unique7get-distinct-role-pairs-roles-single-choice-multiple-interactions-protocol-test
  (is (= 7 (count (get-distinct-role-pairs (get-interactions (single-choice-multiple-interactions-protocol true)))))))

(deftest unique4-roles-single-nested-branch-choice-test
  (is (= 4 (count (get-distinct-role-pairs (get-interactions (single-nested-choice-branch-protocol)))))))

(deftest unique10-roles-multiple-nested-branch-choice-test
  (is (= 10 (count (get-distinct-role-pairs (get-interactions (multiple-nested-branches-protocol true)))))))

(deftest unique3-roles-single-recur-test
  (is (= 5 (count (get-distinct-role-pairs (get-interactions (single-recur-protocol true)))))))

(deftest unique7-roles-nested-recur-protocol-testt
  (is (= 7 (count (get-distinct-role-pairs (get-interactions (nested-recur-protocol true)))))))

(deftest unique6-roles-multiple-nested-recur-protocol-test
  (is (= 6 (count (get-distinct-role-pairs (get-interactions (multiple-nested-recur-protocol)))))))

(deftest unique-minimum-role-pairs-test
  (let [roles (get-distinct-role-pairs (get-interactions (testQuadProtocol true)))
        main (nth roles 0)
        ab (nth roles 1)
        ba (nth roles 2)
        ac (nth roles 3)
        cab (nth roles 4)]
    (is (== 5 (count roles)))
    (is (and (= (:sender main) "main") (= (:receivers main) ["A" "B" "C"])))
    (is (and (= (:sender ab) "A") (= (:receivers ab) "B")))
    (is (and (= (:sender ba) "B") (= (:receivers ba) "A")))
    (is (and (= (:sender ac) "A") (= (:receivers ac) "C")))
    (is (and (= (:sender cab) "C") (= (:receivers cab) ["A" "B"])))
    ))

(deftest unique-minimum-multiple-nested-branches-protocol-role-pairs-test
  (let [roles (get-distinct-role-pairs (get-interactions (multiple-nested-branches-protocol true)))]
    (is (== 10 (count roles)))))

(deftest unique-minimum-single-recur-protocol-role-pairs-test
  (let [roles (get-distinct-role-pairs (get-interactions (single-recur-protocol true)))]
    (is (== 5 (count roles)))))

(deftest two-buyer-protocol-role-test
  (let [roles (get-distinct-role-pairs (get-interactions (two-buyer-protocol true)))]
    (is (== 5 (count roles)))))

(deftest two-buyer-protocol-role-pairs-test
  (let [roles (get-distinct-role-pairs (get-interactions (two-buyer-protocol true)))]
    (is (== 5 (count roles)))))