(ns discourje.async.operationTests
  (:require [clojure.test :refer :all]
            [discourje.async.protocolTestData :refer :all]
            [discourje.core.async.async :refer :all]
            [clojure.core.async :as async]))

(deftest send-test
  (let [channels (generate-infrastructure (testDualProtocol))
        c (get-channel "A" "B" channels)]
    (>!!! c (->message "1" "hello world"))
    (let [m (async/<!! (get-chan c))]
      (is (= "1" (get-label m)))
      (is (= "hello world" (get-content m))))))

(deftest receive-test
  (let [channels (generate-infrastructure (testDualProtocol))
        c (get-channel "A" "B" channels)]
    (async/>!! (get-chan c) (->message "1" "hello world"))
    (let [m (<!!! c "1")]
      (is (= "1" (get-label m)))
      (is (= "hello world" (get-content m))))))

(deftest send-receive-dual-test
  (let [channels (generate-infrastructure (testDualProtocol))
        ab (get-channel "A" "B" channels)
        ba (get-channel "B" "A" channels)
        m1 (->message "1" "Hello B")
        m2 (->message "2" "Hello A")]
    (do
      (>!!! ab m1)
      (let [a->b (<!!! ab "1")]
        (is (= "1" (get-label a->b)))
        (is (= "Hello B" (get-content a->b))))
      (>!!! ba m2)
      (let [b->a (<!!! ba "2")]
        (is (= "2" (get-label b->a)))
        (is (= "Hello A" (get-content b->a)))))))

(deftest send-receive-single-parallel-test
  (let [channels (generate-infrastructure (testSingleParallelProtocol))
        ab (get-channel "A" "B" channels)
        ac (get-channel "A" "C" channels)
        m1 (->message "1" "Hello B and C")]
    (do
      (>!!! [ab ac] m1)
      (let [a->b (<!!! ab "1")
            a->c (<!!! ac "1")]
        (is (= "1" (get-label a->b)))
        (is (= "Hello B and C" (get-content a->b)))
        (is (= (get-label a->c) (get-label a->b)))
        (is (= (get-content a->c) (get-content a->b)))))))