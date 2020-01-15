(ns discourje.async.putTakeTests
  (:require [clojure.test :refer :all]
            [discourje.core.async :refer :all]
            [discourje.async.protocolTestData :refer :all]
            [discourje.async.operationTests :refer :all]
            [discourje.async.parameterizedRecTests :refer :all]))

(deftest put-Take-dual-test
  (let [channels (generate-infrastructure (testDualProtocol true))
        ab (get-channel channels "A" "B")
        ba (get-channel channels "B" "A")
        m1 (->message "1" "Hello B")
        m2 (->message "2" "Hello A")]
    (set-logging)
    (put! ab m1 (fn [x]
                  (take! ab
                         (fn [x] (do
                                   (is (= "Hello B" (get-content x)))
                                   (put! ba m2
                                         (fn [x] (take! ba
                                                        (fn [x]
                                                          (is (= "Hello A" (get-content x)))))))
                                   )))))))