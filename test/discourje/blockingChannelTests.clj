(ns discourje.blockingChannelTests
  (:require [clojure.test :refer :all]
            [clojure.core.async :as async :refer :all]
            [discourje.core :refer :all]))

(def a (chan))

(defn takeBlocking [channel]
  (<!! channel))

(defn putNonBlocking [channel message]
  (go (>! channel message)))

(deftest goMessage
  (putNonBlocking a "test")
  (is (= "test" (takeBlocking a))))