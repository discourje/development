(ns discourje.async.stringifyTests
  (:require [clojure.test :refer :all]
            [discourje.core.async :refer :all]))

(deftest stringify-interaction-test
  (is (= "Interaction - Action: 1, Sender: A, Receivers: B with accepted sends #{}" (to-string (make-interaction "1" "A" "B")))))

(deftest stringify-close-test
  (is (= "Closer from Sender: A to Receiver: B" (to-string (make-closer "A" "B")))))

(deftest stringify-branch-test
  (is (= "Branching with branches - [ Interaction - Action: 1, Sender: A, Receivers: B with accepted sends #{} ][ Interaction - Action: 1, Sender: A, Receivers: B with accepted sends #{} ]" (to-string (get-active-interaction (generate-monitor (create-protocol [(make-choice [[(make-interaction "1" "A" "B")] [(make-interaction "1" "A" "B")]])])))))))

(deftest stringify-parallel-test
  (is (= "Parallel with parallels - [ Interaction - Action: 1, Sender: A, Receivers: B with accepted sends #{} ][ Interaction - Action: 1, Sender: A, Receivers: B with accepted sends #{} ]" (to-string (get-active-interaction (generate-monitor (create-protocol [(make-parallel [[(make-interaction "1" "A" "B")] [(make-interaction "1" "A" "B")]])])))))))

(deftest stringify-identifiable-recur-test
  (is (= "Recur-identifier - name: :rec, option: :recur" (to-string (do-recur :rec)))))

(deftest stringify-channel-test
  (is (= "channel with Provider a, Consumer b and buffer 1" (to-string (generate-channel "a" "b" nil 1)))))