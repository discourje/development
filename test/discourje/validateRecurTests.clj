(ns discourje.validateRecurTests
  (:require [clojure.test :refer :all]
            [discourje.core.monitor :refer :all]
            [discourje.core.dataStructures :refer :all]
            [discourje.core.protocolCore :refer :all]
            [discourje.core.protocol :refer :all])
  (:import (clojure.lang Seqable)
           (discourje.core.dataStructures choice recur! recursion)))

(defn- defineRecurringProtocol []
  (vector (->recursion :y
                       (vector
                         (->receiveM "quoteDiv" "buyer2" "buyer1")
                         (->choice [
                                    (->receiveM "repeat" ["buyer2" "buyer1"] "seller")
                                    (generateRecur :y)
                                    ]
                                   [
                                    (->receiveM "quit" "seller" "buyer2")
                                    (generateRecurStop :y)
                                    ])))))

(defn- findRecurByTag
  "Find a (nested) recursion :end or :recur in the protocol by name"
  [protocol name tag]
  (for [element protocol
        :when (or (instance? recursion element) (instance? choice element) (instance? recur! element))]
    (cond
      (instance? recursion element)
      (findRecurByTag (:protocol element) name tag)
      (instance? choice element)
      (let [trueResult (filter some? (findRecurByTag (:trueBranch element) name tag))
            falseResult (filter some? (findRecurByTag (:falseBranch element) name tag))]
        (if (not (empty? trueResult))
          trueResult
          falseResult))
      (instance? recur! element)
      (when (and
              (= (:name element) name)
              (= (:status element) tag))
        element)
      )))

(defn hasCorrectRecurAndEnd?
  "is the protocol correctly recured and ended?"
  [protocol]
  (let [recurs (distinct (findAllRecursionsInProtocol protocol))]
    (every? true?  (for [rec recurs]
      (let [recursionVector (findRecurByName protocol rec)
            recurElement (first (filter some? (findRecurByTag (:protocol recursionVector) rec :recur)))
            endElement (first (filter some? (findRecurByTag (:protocol recursionVector) rec :end)))]
         (and (not (empty? recurElement)) (not (empty? endElement))))))))
(hasCorrectRecurAndEnd? (defineRecurringProtocol))