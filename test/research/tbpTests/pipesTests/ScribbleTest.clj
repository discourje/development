(ns research.tbpTests.pipesTests.ScribbleTest
  (:require [clojure.test :refer :all]
            [research.twoBuyerProtocol.pipes.Scribble :refer :all]))

(def valid (->message "content" :valid))
(def invalid (->message "CONTENT" :invalid))

(deftest testTag
  (is (= :valid (:tag valid))))

(def validFilter? (fn [x] (= (:tag x) :valid)))


(deftest filterMessage
  (is (= valid (first (filter validFilter? [valid])))))

(deftest filterMessageInvalid
  (is (= nil (first (filter validFilter? [invalid])))))

(deftest filterMessageMyFilter
  (is (= valid (first (filter (filterBy :valid) [valid])))))

(deftest filterMessageInvalidMyFilter
  (is (= nil (first (filter (filterBy :valid) [invalid])))))

(deftest filterMessageIF
  (is (= valid (first (filter (IF :valid (fn [x] (println x))) [valid])))))

(deftest filterMessageInvalidIF
  (is (= nil (first (filter (IF :valid (fn [x] (println x))) [invalid])))))

;(defn filterAndInvoke [tag operation]
;  (if-not (nil? (filterBy tag)) operation))

(deftest strTest
  (is (= (str "true") (str "true"))))