(ns discourje.tbpTests.TBPTests
  (:require [clojure.test :refer :all])
  (:require [discourje.threeBuyerProtocol.ThreeBuyerProtocol :refer :all])
  (:refer-clojure :exclude [send])
  (:import (discourje.core message)))

(deftest testHelloWorldContent
  (is (= (str "Hello World") (:data helloWorldMessage))))

(deftest testHelloWorldNumberContent
  (is (= (int 1234) (:data helloWorldNumber))))
