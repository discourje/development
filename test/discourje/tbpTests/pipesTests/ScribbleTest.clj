(ns discourje.tbpTests.pipesTests.ScribbleTest
  (:require [clojure.test :refer :all]
            [discourje.twoBuyerProtocol.pipes.Scribble :refer :all]))

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

(defn filterAndInvoke [tag operation]
  (if-not (nil? (filterBy tag)) operation))

(deftest strTest
  (is (= (str "true") (str "true"))))

(first (filter
         (filterAndInvoke :valid (str 7))
         [valid]))

(deftest trueFilter1
  (is (= (str "true")
          (filter
           (filterAndInvoke :valid (str "true"))
           [valid]))))


(deftest trueFilter
  (is (= "true" (filter
                  (filterAndInvoke :valid
                                   (str "true"))
                  [valid]))))
