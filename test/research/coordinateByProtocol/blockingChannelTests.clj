(ns discourje.coordinateByProtocol.blockingChannelTests
  (:require [clojure.test :refer :all]
            [clojure.core.async :as async :refer :all]
            [discourje.coordinateByProtocol.core :refer :all]))

(def a (chan))

(defn takeBlocking [channel]
  (<!! channel))

(defn putNonBlocking [channel message]
  (go (>! channel message)))

(deftest goMessage
  (putNonBlocking a "test")
  (is (= "test" (takeBlocking a))))