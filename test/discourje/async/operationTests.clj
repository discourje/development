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