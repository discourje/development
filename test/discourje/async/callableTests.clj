(ns discourje.async.callableTests
  (:require [clojure.test :refer :all]
            [discourje.core.async :refer :all]))

(deftest callable-bla?
  (let [bla ""]
    (is (false? (callable? bla)))))

(deftest callable-callable?
  (is (true? (callable? callable?))))

(deftest callable-fn?
  (is (true? (callable? (fn [label] (= label "yes this test passes"))))))

(deftest trycall-fn
  (let [f (fn [value] (+ value 1))]
  (is (== 2 (f 1)))))