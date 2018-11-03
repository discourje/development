(ns discourje.functionOnAtomThread
  (:require [clojure.test :refer :all]
            [clojure.core.async :as async :refer :all]
            [discourje.core :refer :all]))

(def alice (createParticipant))

;(swap! alice assoc :state (format "InternalState changed %s" (+ (rand-int 30)1)))

(defn consumeStateByEval
  "Swaps participant state value by executing function on thread and setting result in state"
  [participant function]
  (println (clojure.string/upper-case (str "Before concat: " function)))
  (concat function (:state @participant))
  (println (clojure.string/upper-case (str "After concat: " function)))
  (swap! participant assoc :state (eval function)))

(defn consumeStateByEval2
  "Swaps participant state value by executing function on thread and setting result in state"
  [participant function]
  (println (clojure.string/upper-case (str function)))
  (swap! participant assoc :state (eval function :state)))

(defn changeStateByF [participant function]
  (println (clojure.string/upper-case (str function)))
  (swap! participant assoc :state (function)))


(defn toHigherOrder
  "current solution for delaying function eval"
  [f]
  (println f)
  (fn [] (eval f)))

(macroexpand `(sendOff (format "changing state to %s" (str "hello"))))
(sendOff (format "changing state to %s" (str "hello")))


;not working !(changeState alice '(format "changing state to %s" (str "hello")))
(changeStateByF alice (toHigherOrder '(format "changing state to %s" (str "hello"))))
(changeStateByF alice (fn [] (format "changing state to %s" (str "hello"))))
(changeStateByEval alice (sendOff (format "changing state to %s" (str "hello"))))

(consumeStateByEval2 alice (sendOff (fn [x] (clojure.string/upper-case x))))


(deftest changeStateByData
  "changes the state of alice(participant by swapping data with data)"
  (is (= nil (:state @alice)))
  (changeStateByData alice "changedState")
  (is (= "changedState" (:state @alice))))

(changeStateByData alice nil)

(deftest changeStateByFunction
  "changes the state of alice (participant by swapping data with function result"
  (is (= nil (:state @alice)))
  (changeStateByF alice '(format "changing state to %s" (str "hello")))
  (is (= "changing state to hello" (:state @alice))))