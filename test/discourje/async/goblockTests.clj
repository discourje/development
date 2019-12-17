(ns discourje.async.goblockTests
  (:require [clojure.test :refer :all]
            [discourje.core.async :refer :all]
            [discourje.async.protocolTestData :refer :all]
            [discourje.async.operationTests :refer :all]))

(defmacro <!-test
  "Utility method to fix all test cases"
  [channel]
  `(let [~'value (discourje.core.async/<! ~channel)]
    (get-content ~'value)))

(defn <!-!-test
  "Utility method to fix all test cases"
  [channel]
  (let [value (discourje.core.async/<!-! channel)]
    (get-content value)))

(deftest go-send-receive-dual-test
  (let [channels (generate-infrastructure (testDualProtocol true))
        ab (get-channel channels "A" "B")
        ba (get-channel channels "B" "A")
        m1 (->message "1" "Hello B")
        m2 (->message "2" "Hello A")]
    (clojure.core.async/go
          (>! ab m1)
          (let [a->b (<!-test ab)]
            (println a->b)
            (is (= "Hello B" a->b)))
          (>! ba m2)
          (let [b->a (<!-test ba)]
            (is (= "Hello A" b->a)))
          )))