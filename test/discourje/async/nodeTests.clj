(ns discourje.async.nodeTests
  (:require [clojure.test :refer :all]
            [discourje.core.async.async :refer :all]))

(defn generateNodes []
  (let [trans1 (->transition 1 2 "1-2")
        trans2 (->transition 2 1 "2-1")
        trans3 (->transition 2 3 "2-3")
        trans4 (->transition 3 2 "3-2")
        trans5 (->transition 3 1 "3-1")
        trans6 (->transition 1 3 "1-3")

        trans7 (->transition 2 7 "1-7")
        trans8 (->transition 2 8 "1-8")
        trans9 (->transition 2 9 "1-9")

        n1 (->node 1 [trans1 trans2] false)
        n2 (->node 2 [trans3 trans4 trans7 trans8 trans9] false)
        n3 (->node 3 [trans5 trans6] false)]
    [n1 n2 n3]))

(deftest n1-transitions []
                        (let [nodes (generateNodes)]
                          (is (= "1-2" (get-action-label(first (get-output-transitions (first nodes))))))))

(deftest n2-transitions []
                        (let [nodes (generateNodes)]
                          (is (= 4 (count (get-output-transitions (nth nodes 1)))))))