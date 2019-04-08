(ns discourje.async.infraTests
  (:require [clojure.test :refer :all]
            [discourje.async.protocolTestData :refer :all]
            [discourje.core.async :refer :all]))

(deftest dual-interaction-infra-test
  (let [channels (generate-infrastructure (testDualProtocol))]
  (is (= 2 (count channels)))))

(deftest custom-channels-infra-test
  (let [channels [(->channel "A" "B" nil 0 nil) (->channel "B" "A" nil 0 nil)]]
    (generate-infrastructure (testDualProtocol) channels)
    )
  )