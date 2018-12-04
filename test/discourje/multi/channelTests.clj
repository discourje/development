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

(for [x ["buyer1" "buyer2"]] (println x))

(defn contribute?
  "returns true when the received quote 50% or greater"
  [quote div]
  (println (format "received quote: %d and div: %d" quote div))
  (>= (* 100 (float (/ div quote))) 50))

(contribute? 20 10)