(ns discourje.findRecurtests
  (:require [clojure.test :refer :all])
  (require [discourje.core.monitor :refer :all]
           [discourje.core.dataStructures :refer :all])
  (use [discourje.core.core :only [generateChannels]])
  (:import (discourje.core.dataStructures recursion choice)))

(defn flatten-map [path m]
  (if (map? m)
    (mapcat (fn [[k v]] (flatten-map (conj path k) v)) m)
    [[path m]]))

(defn find-in [coll x]
  (->> (flatten-map [] coll)
       (filter (fn [[_ v]] (if (instance? recursion v)
                             (do (println "yes found recur!")
                                 (= (:name v) x))
                             false)))
       (map first)))

(defn- defineRecurringProtocol []
  (vector
    (->receiveM "title" "buyer1" "seller")
    (->receiveM "title" "buyer1" "seller")
    (->receiveM "title" "buyer1" "seller")
    (->receiveM "title" "buyer1" "seller")
    (->choice [(->receiveM "ok" "buyer2" "seller")
               (->receiveM "address" "buyer2" "seller")
               (->recursion :x (vector
                                 (->receiveM "date" "seller" "buyer2")
                                 (generateRecur :x)))

               ]
              [(->receiveM "quit" "buyer2" "seller")
               (generateRecurStop :x)
               ])
    (->recursion :x
                 (vector
                   (->receiveM "title" "buyer1" "seller")
                   (->receiveM "quote" "seller" ["buyer1" "buyer2"])
                   (->receiveM "quoteDiv" "buyer1" "buyer2")
                   (->choice [(->receiveM "ok" "buyer2" "seller")
                              (->receiveM "address" "buyer2" "seller")
                              (->receiveM "date" "seller" "buyer2")
                              (generateRecur :x)
                              ]
                             [(->receiveM "quit" "buyer2" "seller")
                              (generateRecurStop :x)
                              ])))))

(defn- defineRecurringProtocolrec []
  (vector
    (->recursion :x
                 (vector
                   (->receiveM "first" "first" "first")
                   (->choice [

                              (->recursion :y
                                           (vector
                                             (->receiveM "second" "second" "second")
                                             (->choice [
                                                        (generateRecur :y)
                                                        ]
                                                       [
                                                        (generateRecurStop :y)
                                                        ])))

                              ;(->recursion :x
                              ;             (vector
                              ;               (->receiveM "duplicate" "duplicate" "duplicate")
                              ;               (->choice [
                              ;                          (generateRecur :x)
                              ;                          ]
                              ;                         [
                              ;                          (generateRecurStop :x)
                              ;                          ])))

                              (generateRecur :x)
                              ]
                             [
                              (generateRecurStop :x)
                              ])))))

(defn findNestedRecurByName
  ([protocol name result]
   (let [result2 (flatten (vec (conj result [])))]
     (for [element protocol
           :when (or (instance? recursion element) (instance? choice element))]
       (cond
         (instance? recursion element)
         (if (= (:name element) name)
           (flatten (vec (conj result2 [(:name element) (findNestedRecurByName (:protocol element) name result2)])))
           (flatten (vec (conj result2 (findNestedRecurByName (:protocol element) name result2)))))
         (instance? choice element)
         (let [trueResult (findNestedRecurByName (:trueBranch element) name result2)
               falseResult (findNestedRecurByName (:falseBranch element) name result2)]
           (if (not (nil? trueResult))
             (flatten (vec (conj result2 trueResult)))
             (flatten (vec (conj result2 falseResult))))))))))

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

(defn findAllRecursionsInProtocol [protocol]
  (let [x (findAllRecursions protocol [])]
    (vec (first (drop-while empty? x)))))

(defn duplicates? [coll except]
  (let [except (set except)
        filtered (remove #(contains? except %) coll)]
    (not= filtered (distinct filtered))))
(defn containsDuplicates? [definedRecursions]
  (duplicates? definedRecursions ""))
(defn- findRecObject
  "Find a (nested) recursion map in the protocol by name, preserves nested structure in result!"
  [protocol name]
  (for [element protocol
        :when (or (instance? recursion element) (instance? choice element))]
    (cond
      (instance? recursion element)
      (if (= (:name element) name)
        element
        (findRecObject (:protocol element) name))
      (instance? choice element)
      (let [trueResult (findRecObject (:trueBranch element) name)
            falseResult (findRecObject (:falseBranch element) name)]
        (if (not (nil? trueResult))
          trueResult
          falseResult)))))
(defn hasCorrectRecur? [protocol k]
  (for [element protocol]
    (cond
      (instance? recursion element)
      (if (= (:name element) name)
        element
        (hasCorrectRecur? (:protocol element) name))
      (instance? choice element)
      (let [trueResult (hasCorrectRecur? (:trueBranch element) name)
            falseResult (hasCorrectRecur? (:falseBranch element) name)]
        (if (not (nil? trueResult))
          trueResult
          falseResult)))))

(defn recurCorrect? [recursions protocol]
  (every? true? [name recursions]))
(defn endCorrect? [x protocol]
  true)

(defn isProtocolValid? []
  (let [protocol (defineRecurringProtocolrec)
        definedRecursion (findAllRecursionsInProtocol protocol)]
    (and (not (containsDuplicates? definedRecursion)) (recurCorrect? (distinct definedRecursion) protocol) (endCorrect? (distinct definedRecursion) protocol))))
(isProtocolValid?)


(defn findRecurByNames [protocol name]
  (let [x (findNestedRecurByName protocol name [])]
    (vec (first (drop-while empty? x)))))

(findRecurByNames (defineRecurringProtocolrec) :x)

(first (drop-while empty? (flatten (findNestedRecurByName (defineRecurringProtocol) :x))))


(def prot (defineRecurringProtocol))
(println (flatten-map [] prot))

(find-in prot :x)

(defn find-thing [needle haystack]
  (keep-indexed
    #(when
       (= %2 needle) %1)
    haystack))

(defn find-recursion [needle haystack]
  (keep-indexed
    #(when (and (instance? recursion %2) (= (:name %2) needle)) %1
                                                                ;(= %2 needle) %1
                                                                )
    haystack))




(find-recursion :x prot)