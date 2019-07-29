(ns discourje.async.nestedMepMacroTest
  (:require [clojure.test :refer :all]
            [discourje.core.async :refer :all]))

(def inter [(-->> 1 "a" "b")
            (-->> 2 "b" "c")
            (-->> 3 "c" "d")
            (choice [(-->> 4 "1" "2") (-->> 5 "1" "2")]
                    [(-->> 6 "1" "2") (-->> 7 "1" "2")])
            (-->> 9 "d" "b")
            (rec :test
                 (-->> 11 "1" "2")
                 (-->> 12 "1" "3")
                 ;(continue :test)
                 )])

(declare nest-mep)
(defn- assoc-interaction [nth-i it]
  (cond
    (satisfies? interactable it)
    (assoc nth-i :next it)
    (satisfies? branchable it)
    (let [branches (for [b (get-branches it)] (nest-mep (conj b (:next it))))]
      (assoc nth-i :next (assoc (assoc it :next nil) :branches branches)))
    (satisfies? recursable it)
    (let [rec (nest-mep (get-recursion it))]
      (assoc nth-i :next (assoc (assoc it :next nil) :recursion rec)))
    )
  )



(defn- nest-mep [interactions]
  (when-not (nil? interactions)
    (if (>= (count interactions) 2)
      (loop [i (- (count interactions) 2)
             it (last interactions)]
        (if (== 0 i)
          (assoc-interaction (nth interactions i) it)
          (let [linked (assoc-interaction (nth interactions i) it)]
            (recur (- i 1) linked)))
        )
      (first interactions)
      )))
(println (nest-mep inter))



