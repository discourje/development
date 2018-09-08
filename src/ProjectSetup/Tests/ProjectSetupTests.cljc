(ns ProjectSetup.Tests.ProjectSetupTests
  (:require [clojure.test :refer :all])
  (:require [ProjectSetup.ProjectSetup :refer :all]))

(deftest sumTest
  (is (= 6 (sum 1 2 3))))

(deftest flatListTest
  (is (= (1 2 3 4 5 6)) value2 ))

(deftest valueTest
  (is (= 10) value))