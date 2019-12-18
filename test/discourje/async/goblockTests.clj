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

(defmacro <!8-test
  "Utility method to fix all test cases"
  [channel]
  `(let [~'value (discourje.core.async/<!8 ~channel)]
     (get-content ~'value)))

(deftest go-send-receive-dual-test
  (let [channels (generate-infrastructure (testDualProtocol true))
        ab (get-channel channels "A" "B")
        ba (get-channel channels "B" "A")
        m1 (->message "1" "Hello B")
        m2 (->message "2" "Hello A")]
    (is (= "Hello A"
           (clojure.core.async/<!! (go
                                     (>! ab m1)
                                     (<!-test ab)
                                     (>! ba m2)
                                     (<!-test ba)))))))

(deftest go-send-receive-single-multicast-test
  (let [channels (generate-infrastructure (testSingleMulticastProtocol))
        ab (get-channel channels "A" "B")
        ac (get-channel channels "A" "C")
        m1 (->message "1" "Hello B and C")]
    (is (= "Hello B and C"
           (clojure.core.async/<!! (go
                                     (>! [ab ac] m1)
                                     (let [a->b (<!-test ab)
                                           a->c (<!-test ac)]
                                       a->c)
                                     ))))
    (is (nil? (get-active-interaction (get-monitor ab))))))

(deftest go-send-receive-multicast-protocol-test
  (let [channels (generate-infrastructure (testMulticastProtocol true))
        ab (get-channel channels "A" "B")
        ba (get-channel channels "B" "A")
        ac (get-channel channels "A" "C")
        ca (get-channel channels "C" "A")
        cb (get-channel channels "C" "B")
        m1 (->message "1" "A->B")
        m2 (->message "2" "B->A")
        m3 (->message "3" "A->C")
        m4 (->message "4" "C->A-B")]
    (is (= "C->A-B"
           (clojure.core.async/<!! (go
                                     (do
                                       (println 1)
                                       (>! ab m1)
                                       (println 2)
                                       (<!-test ab)
                                       (println 3)
                                       (>! ba m2)
                                       (println 4)
                                       (<!-test ba)
                                       (println 5)
                                       (>! ac m3)
                                       (println 6)
                                       (<!-test ac)
                                       (println 7)
                                       (>! [ca cb] m4)
                                       (println 8)
                                       (<!-test ca)
                                       (println 9)
                                       (<!-test cb)
                                       (println 10))))))
    (is (nil? (get-active-interaction (get-monitor ab))))))