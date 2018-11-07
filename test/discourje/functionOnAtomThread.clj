(ns discourje.functionOnAtomThread
  (:require [clojure.test :refer :all]
            [clojure.core.async :as async :refer :all]
            [discourje.core :refer :all]
            [discourje.samples :refer :all]))

(def testParticipant (createParticipant))

;(swap! alice assoc :state (format "InternalState changed %s" (+ (rand-int 30)1)))



(defn changeStateByF [participant function]
  (println (clojure.string/upper-case (str function)))
  (swap! participant assoc :state (function)))

(macroexpand-1 `(sendOffData (format "changing state to %s" (str "hello"))))
;(def ap(macroexpand-1 `(sendOff (format "changing state to %s" (str "hello")))))
;ap
;(deftest macroExpandSendOffTests
;  "check if the macro expands correctly"
;  (is (= ap;(macroexpand-1 `(sendOff (format "changing state to %s" (str "hello"))))
;        (quote (clojure.core/format "changing state to %s" (clojure.core/str "hello"))))))
(sendOffData (format "changing state to %s" (str "hello")))

(changeStateByF testParticipant (fn [] (format "changing state to %s" (str "hello"))))
(changeStateByEval testParticipant (sendOffData (format "changing state to %s" (str "hello"))))


(def carol (createParticipant))

(deftest changeStateByDataTest
  "changes the state of alice(participant by swapping data with data)"
  (changeStateByData carol "changedState")
  (is (= "changedState" (:state @carol))))

(def alice(createParticipant))
(changeStateByEval alice (sendOffData (format "changing state to %s" (str "hello"))))

(deftest changeStateByDelayedFunction
  "uses our sendoff macro to delay function evaluation to alice thread"
  (is (= "changing state to hello" (:state @alice))))

(changeStateByEval alice (sendOffData (clojure.string/upper-case (:state @alice))))

(deftest changeStateByDelayedFunctionConsumingState
  "changes the state of alice, by delaying function evaluation which consumes the previous state (as function argument)"

  (is (= "CHANGING STATE TO HELLO" (:state @alice))))

(def bob (createParticipant))
(deftest changeStateByFunction
  "changes the state of bob (participant by swapping data with function result"
  (changeStateByF bob '(format "changing state to %s" (str "hello")))
  (is (= "changing state to hello" (:state @bob))))


;(changeStateByF alice (toHigherOrder '(format "changing state to %s" (str "hello"))))
;(defn toHigherOrder
;  "current solution for delaying function eval"
;  [f]
;  (println f)
;  (fn [] (eval f)))