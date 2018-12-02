(ns discourje.multi.channelTests
  (:require [clojure.test :refer :all]
            [clojure.core.async :refer :all]))
(def a (chan))
(close! a)



(go (>! a "a"))
(defn getValue [x]
  x)
(take! a (fn [x] x))

(let [returnVal (atom nil)
      c (chan)]
  (put! c "value on channel")
  (take! c (fn [x] (reset! returnVal x)))
  @returnVal)

;this works
(def chann (chan))
(def returnVal (atom nil))
(take! chann (fn [x] (reset! returnVal x)))
(put! chann "bla")
(println returnVal)
;;;

(deftest nonBlockingTake
  (let [a (chan)
        returnV (atom nil)
        callback (fn [x] (reset! returnV x))]
    (go (>! a "hello"))
    (take! a callback)
    (is (= "hello" @returnV))
    (close! a)))

(for [x [1 2 3]] (println x))