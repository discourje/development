(ns discourje.tbpTests.pipesTests.buyer1Tests
  (:require [clojure.test :refer :all]
            [clojure.core.async :as async :refer :all]
            [discourje.twoBuyerProtocol.pipes.participants.buyer1 :as buyer1 :refer :all]
            [discourje.twoBuyerProtocol.pipes.util :as util :refer :all]))

(deftest randomBookTest
  (let [b (getRandomBook)]
    (is (or
          (= b "The Joy of Clojure")
          (= b "Mastering Clojure Macros")
          (= b "Clojure Programming")))))

(deftest randomBookContains
  (is true (contains? bookCollection getRandomBook)))