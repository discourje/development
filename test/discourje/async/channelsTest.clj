(ns discourje.async.channelsTest
  (:require [clojure.test :refer :all]
            [discourje.async.protocolTestData :refer :all]
            [discourje.core.async :refer :all]))

;;;; Sung: Moved here from macroTests.clj
(deftest create-channel-test
  (let [fnChan (generate-channel "a" "b" 1)
        macroChan (chan "a" "b" 1)]
    (is (= (get-provider fnChan) (get-provider macroChan)))
    (is (= (get-consumer fnChan) (get-consumer macroChan)))
    (is (= (get-buffer fnChan) (get-buffer macroChan)))))

(deftest equal-senders-test
  (let [chans[(new-channel 1 2 nil 1 nil)
         (new-channel 1 3 nil 1 nil)
         (new-channel 1 4 nil 1 nil)]]
    (is true? (equal-senders? chans))))

(deftest not-equal-senders-test
  (let [chans[(new-channel 1 2 nil 1 nil)
              (new-channel 5 3 nil 1 nil)
              (new-channel 1 4 nil 1 nil)]]
    (is false? (equal-senders? chans))))

(deftest not-equal-senders-when-empty-test
  (let [chans[]]
    (is false? (equal-senders? chans))))

(deftest dual-channels-test
  (let [roles (get-distinct-role-pairs (get-interactions (testDualProtocol true)))
        channels (generate-minimum-channels roles nil 1)]
    (is (= 2 (count channels)))))

(deftest triple-channels-test
  (let [roles (get-distinct-role-pairs (get-interactions (testTripleProtocol true)))
        channels (generate-minimum-channels roles nil 1)]
    (is (= 3 (count channels)))))

(deftest triple-channels-roles-test
  (let [roles (get-distinct-role-pairs (get-interactions (testMulticastProtocol true)))
        channels (generate-minimum-channels roles nil 1)]
    (is (= 5 (count channels)))))

(deftest quad-channels-roles-test
  (let [roles (get-distinct-role-pairs (get-interactions (testQuadProtocol true)))
        channels (generate-minimum-channels roles nil 1)]
    (is (= 8 (count channels)))))

(deftest single-choice-protocol-roles-test
  (let [roles (get-distinct-role-pairs (get-interactions (single-choice-protocol)))
        channels (generate-minimum-channels roles nil 1)]
    (is (= 2 (count channels)))))

(deftest single-choice-in-middle-protocol-roles-test
  (let [roles (get-distinct-role-pairs (get-interactions (single-choice-in-middle-protocol true)))
        channels (generate-minimum-channels roles nil 1)]
    (is (= 6 (count channels)))))

(deftest single-choice-5branches-protocol-roles-test
  (let [roles (get-distinct-role-pairs (get-interactions (single-choice-5branches-protocol true)))
        channels (generate-minimum-channels roles nil 1)]
    (is (= 6 (count channels)))))

(deftest dual-choice-protocol-roles-test
  (let [roles (get-distinct-role-pairs (get-interactions (dual-choice-protocol true)))
        channels (generate-minimum-channels roles nil 1)]
    (is (= 5 (count channels)))))

(deftest single-choice-multiple-interactions-protocol-roles-test
  (let [roles (get-distinct-role-pairs (get-interactions (single-choice-multiple-interactions-protocol true)))
        channels (generate-minimum-channels roles nil 1)]
    (is (= 6 (count channels)))))

(deftest single-nested-choice-branch-protocol-roles-test
  (let [roles (get-distinct-role-pairs (get-interactions (single-nested-choice-branch-protocol)))
        channels (generate-minimum-channels roles nil 1)]
    (is (= 4 (count channels)))))

(deftest multiple-nested-branches-protocol-roles-test
  (let [roles (get-distinct-role-pairs (get-interactions (multiple-nested-branches-protocol true)))
        channels (generate-minimum-channels roles nil 1)]
    (is (= 12 (count channels)))))

(deftest single-recur-protocol-roles-test
  (let [roles (get-distinct-role-pairs (get-interactions (single-recur-protocol true)))
        channels (generate-minimum-channels roles nil 1)]
    (is (= 4 (count channels)))))

(deftest nested-recur-protocol-roles-test
  (let [roles (get-distinct-role-pairs (get-interactions (nested-recur-protocol true)))
        channels (generate-minimum-channels roles nil 1)]
    (is (= 6 (count channels)))))

(deftest multiple-nested-recur-protocol-roles-test
  (let [roles (get-distinct-role-pairs (get-interactions (multiple-nested-recur-protocol)))
        channels (generate-minimum-channels roles nil 1)]
    (is (= 6 (count channels)))))

(deftest minimum-amount-quad-channels-roles-test
  (let [roles (get-distinct-role-pairs (get-interactions (testQuadProtocol true)))
        channels (generate-minimum-channels roles nil 1)]
    (is (= 8 (count channels)))))

(deftest minimum-amount-multiple-nested-branches-protocol-roles-test
  (let [roles (get-distinct-role-pairs (get-interactions (multiple-nested-branches-protocol true)))
        channels (generate-minimum-channels roles nil 1)]
    (is (= 12 (count channels)))))

(deftest minimum-amount-single-recur-protocol-roles-test
  (let [roles (get-distinct-role-pairs (get-interactions (single-recur-protocol true)))
        channels (generate-minimum-channels roles nil 1)]
    (is (= 4 (count channels)))))

(deftest minimum-amount-two-buyer-protocol-roles-test
  (let [roles (get-distinct-role-pairs (get-interactions (two-buyer-protocol true)))
        channels (generate-minimum-channels roles nil 1)]
    (is (= 5 (count channels)))))
(deftest minimum-amount-testSingleParallelProtocol-roles-test
  (let [roles (get-distinct-role-pairs (get-interactions (testSingleMulticastProtocol)))
        channels (generate-minimum-channels roles nil 1)]
    (is (= 2 (count channels)))))

(deftest minimum-amount-parallel-after-interaction-roles-test
  (let [roles (get-distinct-role-pairs (get-interactions (parallel-after-interaction true)))
        channels (generate-minimum-channels roles nil 1)]
    (println channels)
    (is (= 2 (count channels)))))

(deftest parallel-after-rec-with-after-rec-roles-test
  (let [roles (get-distinct-role-pairs (get-interactions (parallel-after-rec-with-after-rec true)))
        channels (generate-minimum-channels roles nil 1)]
    (is (= 2 (count channels)))))

(deftest rec-with-parallel-with-choice-multicast-and-close-rec-roles-test
  (let [roles (get-distinct-role-pairs (get-interactions (rec-with-parallel-with-choice-multicast-and-close true)))
        channels (generate-minimum-channels roles nil 1)]
    (is (= 4 (count channels)))))
