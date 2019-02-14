(ns discourje.async.infraTests
  (:require [clojure.test :refer :all]
            [discourje.async.protocolTestData :refer :all]
            [discourje.core.async.async :refer :all]))

(deftest dual-interaction-infra-test
  (let [channels (generate-infrastructure testDualProtocol)]
  (is (= 2 (count channels)))))