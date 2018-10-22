(ns discourje.twoBuyerProtocol.pipes.Scribble
  (:require [clojure.core.async :as async :refer :all]
            [clojure.core :refer :all]))

(def b1 (chan))
(def b2 (chan))

(defrecord message [content tag])
(def validMessage (->message "valid" :valid))
(def invalidMessage (->message "invalid" :invalid))

(defn filterBy
  "Creates a function to compare tags of messages, this function can be supplied to filter."
  [tag]
  (fn [x] (= (:tag x) tag)))

(defn IF [tag operation]
  (fn [x]
    (let [condition (= (:tag x) tag)]
      (if condition (operation x))
      condition)))

(defn createPipeline
  "Test for pipeline to use transducer as operation"
  [from to operation]
  (pipeline 1 to operation from))

(go (>! b1 invalidMessage))
(go (>! b1 validMessage))


(createPipeline b1 b2 (filterBy :valid))

(go (println :content (<! b2)))




