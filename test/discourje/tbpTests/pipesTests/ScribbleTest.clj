(ns discourje.tbpTests.pipesTests.ScribbleTest
  (:require [clojure.test :refer :all]
            [discourje.twoBuyerProtocol.pipes.Scribble :refer :all]))

(def valid (->message "content" :valid))
(def invalid (->message "CONTENT" :invalid))


(deftest testTag
  (is (= :valid (:tag valid))))


(def validFilter? (fn [x] (= (:tag x) :valid)))

(deftest tagFilertt
  (is (= true (validFilter? valid))))

(deftest macroTest
  (is (= true (filterM :valid) valid)))

(deftest filterMessage
  (is (= true (filter validFilter?))))

(def filterTest (filter (fn [x] (= (:tag x) :valid))))

(deftest filtering
  (is (= true (filter (fn [] (= (:tag valid) :valid))))))


(deftest filterOnlyValid
  (is (= true (filter (fn [x] (= (:tag x) :valid))))))

(filter
  (fn [x]
    (= (:tag x) :valid)) [valid])

(deftest filterValid
  (is (= true (filterTest valid))))

(filter (fn [x]
          (= (count x) 1))
        ["a" "aa" "b" "n" "f" "lisp" "clojure" "q" ""])