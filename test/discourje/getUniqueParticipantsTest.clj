(ns discourje.getUniqueParticipantsTest
  (:require [clojure.test :refer :all]
            [discourje.core.dataStructures :refer :all]
            [discourje.core.monitor :refer :all])
  (:import (clojure.lang Seqable)
           (discourje.core.dataStructures recursion recur! choice sendM receiveM)))

(defn- defineRecurringProtocol []
  (vector (->recursion :x
                       (vector
                         (->sendM "title" "buyer1" "seller")
                         (->receiveM "title" "seller" "buyer1")
                         (->sendM "quote" "seller" ["buyer1" "buyer2"])
                         (->receiveM "quote" ["buyer1" "buyer2"] "seller")
                         (->sendM "quoteDiv" "buyer1" "buyer2")
                         (->receiveM "quoteDiv" "buyer2" "buyer1")
                         (->choice [
                                    (->sendM "ok" "buyer2" "seller")
                                    (->choice [
                                               (->sendM "address" "buyer2" "seller")
                                               (->receiveM "ok" "seller" "buyer2")
                                               (->receiveM "address" "seller" "buyer2")
                                               ]
                                              [
                                               (->receiveM "ok" "seller" "buyer2")
                                               (->sendM "address" "buyer2" "seller")
                                               (->receiveM "address" "seller" "buyer2")
                                               ])
                                    (->sendM "date" "seller" "buyer2")
                                    (->receiveM "date" "buyer2" "seller")
                                    (->sendM "repeat" "buyer2" ["seller" "buyer1"])
                                    (->receiveM "repeat" ["seller" "buyer1"] "buyer2")
                                    (generateRecur :x)
                                    ]
                                   [
                                    (->sendM "quit" "buyer2" "seller")
                                    (->receiveM "quit" "seller" "buyer2")
                                    (generateRecurStop :x)
                                    ])))))

(defn- findAllParticipants
  "List all sender and receivers in the protocol"
  [protocol result]
   (let [result2 (flatten (vec (conj result [])))]
     (conj result2 (flatten (for [element protocol]
       (cond
         (instance? recursion element)
         (flatten (vec (conj result2 (findAllParticipants (:protocol element) result2))))
         (instance? choice element)
         (let [trueResult (findAllParticipants (:trueBranch element) result2)
               falseResult (findAllParticipants (:falseBranch element) result2)]
           (if (not (nil? trueResult))
             (flatten (vec (conj result2 trueResult)))
             (flatten (vec (conj result2 falseResult)))))
         (or (instance? sendM element) (instance? receiveM element))
         (do
           (if (instance? Seqable (:to element))
               (conj result2 (flatten (:to element)) (:from element))
             (conj result2 (:to element) (:from element))))))))))

(defn getDistinctParticipants
  "Get all distinct senders and receivers in the protocol"
  [monitors]
  (vec (filter some? (distinct (first (findAllParticipants monitors []))))))

(println (getDistinctParticipants (defineRecurringProtocol)))


(defn- defineSequenceProtocol
  "This function will generate a vector with 4 monitors to send and receive the greet message.
  Notice how send and receivers are defined separately in order to allow for sequencing of actions!"
  []
  (vector
    (->sendM "greet" "alice" "bob")
    (->sendM "greet" "alice" "carol")
    (->receiveM "greet" "bob" "alice")
    (->receiveM "greet" "carol" "alice")))
(println (getDistinctParticipants (defineSequenceProtocol)))

