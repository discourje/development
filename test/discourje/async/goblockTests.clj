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

(deftest go-send-receive-multicast-protocol-test
  (let [channels (generate-infrastructure (testMulticastProtocol true))
        ab (get-channel channels "A" "B")
        ba (get-channel channels "B" "A")
        ac (get-channel channels "A" "C")
        ca (get-channel channels "C" "A")
        cb (get-channel channels "C" "B")]
    (is (= "C->A-B"
           (clojure.core.async/<!! (go
                                     (>! ab (->message "1" "A->B"))
                                     (<!-test ab)
                                     (>! ba (->message "2" "B->A"))
                                     (<!-test ba)
                                     (>! ac (->message "3" "A->C"))
                                     (<!-test ac)
                                     (>! [ca cb] (->message "4" "C->A-B"))
                                     (<!-test ca)
                                     (<!-test cb)))))
    (is (nil? (get-active-interaction (get-monitor ab))))))