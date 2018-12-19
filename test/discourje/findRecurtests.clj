(ns discourje.findRecurtests
  (:require [clojure.test :refer :all])
  (require [discourje.core.monitor :refer :all])
  (use [discourje.core.core :only [generateChannels]])
  (:import (discourje.core.monitor recursion choice)))

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
    (->monitor "title" "buyer1" "seller")
    (->monitor "title" "buyer1" "seller")
    (->monitor "title" "buyer1" "seller")
    (->monitor "title" "buyer1" "seller")
    (->choice [(->monitor "ok" "buyer2" "seller")
               (->monitor "address" "buyer2" "seller")
               (->recursion :y (vector
                                 (->monitor "date" "seller" "buyer2")
                                 (->recur! :y)))

               ]
              [(->monitor "quit" "buyer2" "seller")
               (->end! :y)
               ])
    (->recursion :x
                 (vector
                   (->monitor "title" "buyer1" "seller")
                   (->monitor "quote" "seller" ["buyer1" "buyer2"])
                   (->monitor "quoteDiv" "buyer1" "buyer2")
                   (->choice [(->monitor "ok" "buyer2" "seller")
                              (->monitor "address" "buyer2" "seller")
                              (->monitor "date" "seller" "buyer2")
                              (->recur! :x)
                              ]
                             [(->monitor "quit" "buyer2" "seller")
                              (->end! :x)
                              ])))))

(defn findNestedRecurByName [protocol name]
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

(defn findRecurByName [protocol name]
  (let [x (findNestedRecurByName protocol name)]
  (first (drop-while empty? (flatten x)))))

(findRecurByName (defineRecurringProtocol) :y)

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