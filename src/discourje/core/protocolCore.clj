(ns discourje.core.protocolCore
  (:require [discourje.core.dataStructures :refer :all] [clojure.core.async])
  (:import (clojure.lang PersistentQueue)
           (discourje.core.dataStructures recursion recur! choice)))


;Defines a communication channel with a sender, receiver (strings), a channel Async.Chan and a queue for receivers.
;We need a receivers queue to allow for multiple send! operations on the same channel without taking values from it by registered receives
;We could also allow for blocking recv! now, without callbacks, but this needs testing!
(defrecord communicationChannel [sender receiver channel receivingQueue])

(defn- generateChannel
  "function to generate a channel between sender and receiver"
  [sender receiver]
  (->communicationChannel sender receiver (clojure.core.async/chan) (atom PersistentQueue/EMPTY)))

(defn uniqueCartesianProduct
  "Generate channels between all participants and filters out duplicates e.g.: buyer1<->buyer1"
  [x y]
  (filter some?
          (for [x x y y]
            (when (not (identical? x y))
              (vector x y)))))

(defn generateChannels
  "Generates communication channels between all participants"
  [participants]
  (map #(apply generateChannel %) (uniqueCartesianProduct participants participants)))

;an instance of a protocol consists of a collection of channels, protocol definition and a monitor flagged active
(defrecord protocolInstance [channels protocol activeMonitor template])

(defn- findAllRecursions
  ([protocol result]
   (let [result2 (flatten (vec (conj result [])))]
     (for [element protocol
           :when (or (instance? recursion element) (instance? choice element))]
       (cond
         (instance? recursion element)
         (flatten (vec (conj result2 [(:name element) (findAllRecursions (:protocol element) result2)])))
         (instance? choice element)
         (let [trueResult (findAllRecursions (:trueBranch element) result2)
               falseResult (findAllRecursions (:falseBranch element) result2)]
           (if (not (nil? trueResult))
             (flatten (vec (conj result2 trueResult)))
             (flatten (vec (conj result2 falseResult))))))))))

(defn- findNestedRecurByName
  "Find a (nested) recursion map in the protocol by name, preserves nested structure in result!"
  [protocol name]
  (for [element protocol
        :when (or (instance? recursion element) (instance? choice element))]
    (cond
      (instance? recursion element)
      (if (= (:name element) name)
        element
        (findNestedRecurByName (:protocol element) name))
      (instance? choice element)
      (let [trueResult (findNestedRecurByName (:trueBranch element) name)
            falseResult (findNestedRecurByName (:falseBranch element) name)]
        (if (not (nil? trueResult))
          trueResult
          falseResult)))))

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
        element))))

(defn duplicates?
  "returns true when duplicates inside collection"
  [coll except]
  (let [except (set except)
        filtered (remove #(contains? except %) coll)]
    (not= filtered (distinct filtered))))

(defn containsDuplicates?
  "checks the recursion vector for recursions"
  [definedRecursions]
  (duplicates? definedRecursions ""))

(defn findAllRecursionsInProtocol
  "find all recursions inside protocol definition, this will include duplicates to later check for"
  [protocol]
  (let [x (findAllRecursions protocol [])]
    (vec (first (drop-while empty? x)))))

(defn findRecurByName
  "Find a (nested) recursion map in the protocol, returns the recursion map directly!"
  [protocol name]
  (println name)
  (let [x (findNestedRecurByName protocol name)]
    (first (drop-while empty? (flatten x)))))

(defn- hasCorrectRecurAndEnd?
  "is the protocol correctly recured and ended?"
  [protocol definedRecursions]
  (let [recurs (distinct definedRecursions)]
    (every? true?  (for [rec recurs]
                     (let [recursionVector (findRecurByName protocol rec)
                           recurElement (first (filter some? (findRecurByTag (:protocol recursionVector) rec :recur)))
                           endElement (first (filter some? (findRecurByTag (:protocol recursionVector) rec :end)))]
                       (and (not (empty? recurElement)) (not (empty? endElement))))))))

(defn isProtocolValid?
  "returns true when there are no duplicate recursion definitions and,=>
  we will include checking for proper recur! [recur, end] definitions!"
  [protocol]
  (let [definedRecursion (findAllRecursionsInProtocol protocol)]
    (and (not (containsDuplicates? definedRecursion)) (hasCorrectRecurAndEnd? protocol definedRecursion))))

(defn- validateRecursion
  "checks the protocol for duplicate recursion definitions and if recur/ended correctly"
  [monitors]
  (isProtocolValid? monitors))