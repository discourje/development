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

(deftest getChannelTest
  (let [channels (generateChannels [1 2 3])
        channel1-2 (getChannel 1 2 channels)]
    (is (= true (and (= 1 (:sender channel1-2))
                     (= 2 (:receiver channel1-2)))))))

(deftest duplicateChannelIsNilTest
  (let [channels (generateChannels [1 2 3])
        channel1-2 (getChannel 1 1 channels)]
    (is (= nil channel1-2))))