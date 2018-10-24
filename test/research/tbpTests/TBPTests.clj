(ns research.tbpTests.TBPTests
  (:require [clojure.test :refer :all])
  (:require [research.threeBuyerProtocol.ThreeBuyerProtocol :refer :all])
  (:refer-clojure :exclude [send])
  (:import (research.core message)))

(deftest testHelloWorldContent
  (is (= (str "Hello World") (:data helloWorldMessage))))

(deftest testHelloWorldNumberContent
  (is (= (int 1234) (:data helloWorldNumber))))
