(ns discourje.async.infraTests
  (:require [clojure.test :refer :all]
            [discourje.async.protocolTestData :refer :all]
            [discourje.core.async :refer :all]))

(deftest dual-interaction-infra-test
  (let [channels (generate-infrastructure (testDualProtocol true))]
    (is (= 2 (count (get-channels channels))))))

(deftest custom-channels-infra-test
  (let [channels [(new-channel "A" "B" nil 0 nil) (new-channel "B" "A" nil 0 nil)]]
    (generate-infrastructure (testDualProtocol true) channels)
    )
  )

(deftest single-parallel-infra-test
  (let [channels (generate-infrastructure (testSingleMulticastProtocol))]
    (println channels)
    (is (= 2 (count (get-channels channels))))))

(deftest two-workers-simple
  (let [channels (generate-infrastructure
                   (create-protocol [(make-interaction "1" "A" ["B" "C"])]))]
    (println channels)
    (is (= 2 (count (get-channels channels))))))