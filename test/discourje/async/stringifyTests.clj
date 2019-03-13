(ns discourje.async.stringifyTests
  (:require [clojure.test :refer :all]
            [discourje.core.async.async :refer :all]))

(deftest stringify-interaction-test
  (is(= "Interaction - Action: 1, Sender: A, Receivers: B" (to-string (-->> "1" "A" "B")))))

(deftest stringify-branch-test
    (is(= "Branching with first branches - [ Interaction - Action: 1, Sender: A, Receivers: B ][ Interaction - Action: 1, Sender: A, Receivers: B ]" (to-string (make-choice [[(-->> "1" "A" "B")][(-->> "1" "A" "B")]])))))

(deftest stringify-recursion-test
  (is(= "Recursion name: :rec, with first in recursion- Interaction - Action: 1, Sender: A, Receivers: B" (to-string (make-recursion :rec [(-->> "1" "A" "B")])))))

(deftest stringify-identifiable-recur-test
  (is(= "Recur-identifier - name: :rec, option: :recur" (to-string (do-recur :rec)))))