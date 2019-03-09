(ns discourje.async.channelsTest
  (:require [clojure.test :refer :all]
            [discourje.async.protocolTestData :refer :all]
            [discourje.core.async.async :refer :all]))

(deftest equal-senders-test
  (let [chans[(->channel 1 2 nil nil nil)
         (->channel 1 3 nil nil nil)
         (->channel 1 4 nil nil nil)]]
    (is true? (equal-senders? chans))))

(deftest not-equal-senders-test
  (let [chans[(->channel 1 2 nil nil nil)
              (->channel 5 3 nil nil nil)
              (->channel 1 4 nil nil nil)]]
    (is false? (equal-senders? chans))))

(deftest not-equal-senders-when-empty-test
  (let [chans[]]
    (is false? (equal-senders? chans))))

(deftest dual-channels-test
  (let [roles (get-distinct-roles (get-interactions (testDualProtocol)))
        channels (generate-channels roles nil 1)]
    (is (= 2 (count channels)))))

(deftest triple-channels-test
  (let [roles (get-distinct-roles (get-interactions (testTripleProtocol)))
        channels (generate-channels roles nil 1)]
    (is (= 6 (count channels)))))

(deftest triple-channels-roles-test
  (let [roles (get-distinct-roles (get-interactions (testParallelProtocol)))
        channels (generate-channels roles nil 1)]
    (is (= 6 (count channels)))))

(deftest quad-channels-roles-test
  (let [roles (get-distinct-roles (get-interactions (testQuadProtocol)))
        channels (generate-channels roles nil 1)]
    (is (= 12 (count channels)))))

(deftest single-choice-protocol-roles-test
  (let [roles (get-distinct-roles (get-interactions (single-choice-protocol)))
        channels (generate-channels roles nil 1)]
    (is (= 6 (count channels)))))

(deftest single-choice-in-middle-protocol-roles-test
  (let [roles (get-distinct-roles (get-interactions (single-choice-in-middle-protocol)))
        channels (generate-channels roles nil 1)]
    (is (= 20 (count channels)))))

(deftest single-choice-5branches-protocol-roles-test
  (let [roles (get-distinct-roles (get-interactions (single-choice-5branches-protocol)))
        channels (generate-channels roles nil 1)]
    (is (= 42 (count channels)))))

(deftest dual-choice-protocol-roles-test
  (let [roles (get-distinct-roles (get-interactions (dual-choice-protocol)))
        channels (generate-channels roles nil 1)]
    (is (= 20 (count channels)))))

(deftest single-choice-multiple-interactions-protocol-roles-test
  (let [roles (get-distinct-roles (get-interactions (single-choice-multiple-interactions-protocol)))
        channels (generate-channels roles nil 1)]
    (is (= 12 (count channels)))))

(deftest single-nested-choice-branch-protocol-roles-test
  (let [roles (get-distinct-roles (get-interactions (single-nested-choice-branch-protocol)))
        channels (generate-channels roles nil 1)]
    (is (= 20 (count channels)))))

(deftest multiple-nested-branches-protocol-roles-test
  (let [roles (get-distinct-roles (get-interactions (multiple-nested-branches-protocol)))
        channels (generate-channels roles nil 1)]
    (is (= 90 (count channels)))))

(deftest single-recur-protocol-roles-test
  (let [roles (get-distinct-roles (get-interactions (single-recur-protocol)))
        channels (generate-channels roles nil 1)]
    (is (= 6 (count channels)))))

(deftest nested-recur-protocol-roles-test
  (let [roles (get-distinct-roles (get-interactions (nested-recur-protocol)))
        channels (generate-channels roles nil 1)]
    (is (= 20 (count channels)))))

(deftest multiple-nested-recur-protocol-roles-test
  (let [roles (get-distinct-roles (get-interactions (multiple-nested-recur-protocol)))
        channels (generate-channels roles nil 1)]
    (is (= 30 (count channels)))))

(deftest minimum-amount-quad-channels-roles-test
  (let [roles (get-distinct-role-pairs (get-interactions (testQuadProtocol)))
        channels (generate-minimum-channels roles nil 1)]
    (is (= 8 (count channels)))))

(deftest minimum-amount-multiple-nested-branches-protocol-roles-test
  (let [roles (get-distinct-role-pairs (get-interactions (multiple-nested-branches-protocol)))
        channels (generate-minimum-channels roles nil 1)]
    (is (= 12 (count channels)))))

(deftest minimum-amount-single-recur-protocol-roles-test
  (let [roles (get-distinct-role-pairs (get-interactions (single-recur-protocol)))
        channels (generate-minimum-channels roles nil 1)]
    (is (= 4 (count channels)))))