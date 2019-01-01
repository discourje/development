(ns discourje.QueueTests
  (:require [clojure.test :refer :all])
  (:import (clojure.lang PersistentQueue)))

(deftest peekTest
  (let [q (atom PersistentQueue/EMPTY)]
    (reset! q (conj @q "hello"))
    (is (and (= "hello" (peek @q)) (= (empty? @q) false)))))

(deftest popTest
  (let [q (atom PersistentQueue/EMPTY)]
    (reset! q (conj @q "hello"))
    (let [value (peek @q)]
      (reset! q (pop @q))
    (is (and (= "hello" value) (= (empty? @q) true))))))

