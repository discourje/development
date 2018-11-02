(ns discourje.functionOnAgentThread
  (:require [clojure.test :refer :all]
            [clojure.core.async :as async :refer :all]
            [discourje.core :refer :all]))

(def alice (createParticipant))

;(swap! alice assoc :state (format "InternalState changed %s" (+ (rand-int 30)1)))

(defn changeState [participant data]
  (println data)
  (swap! participant assoc :state data))

(defn changeStateByF [participant function]
  (println (clojure.string/upper-case (str function)))
  (swap! participant assoc :state (function)))

(defn toHigherOrder [f]
  (println f)
  (fn [] f))

(changeStateByF alice (toHigherOrder '(format "changing state to %s" (str "hello"))))
(changeStateByF alice (fn [](format "changing state to %s" (str "hello"))))


(deftest changeStateByData
  "changes the state of alice(participant by swapping data with data)"
  (is (= nil (:state @alice)))
  (changeState alice "changedState")
  (is (= "changedState" (:state @alice))))

(changeState alice nil)

(deftest changeStateByFunction
  "changes the state of alice (participant by swapping data with function result"
  (is (= nil (:state @alice)))
  (changeStateByF alice '(format "changing state to %s" (str "hello")))
  (is (= "changing state to hello" (:state @alice))))