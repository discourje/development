(ns discourje.multiparty.coreTests
  (:require [clojure.test :refer :all]
            [discourje.multiparty.core :refer :all]))

(deftest generateChannelsTestAmount3
  (let [participants [1 2 3]]
    (is (= 6 (count (generateChannels participants))))))

(deftest generateChannelsTestAmount4
  (let [participants [1 2 3 4]]
    (is (= 12 (count (generateChannels participants))))))

(deftest generateChannelsTestDistinct
  (let [participants [1 2 3]]
    (is (= true (distinct? (generateChannels participants))))))