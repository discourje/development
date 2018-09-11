(ns projectSetup.testCheckConfig
  (:require [clojure.test :refer :all])
  (:require [projectSetup.testCheck :refer :all])
  (:require [clojure.test.check.generators :as gen])
  (:require [clojure.test.check.properties :as prop])
  (:require [clojure.test.check :as tc]))


(def testSum
  (prop/for-all [v (gen/vector gen/int)]
                (== (reduce + v) (apply sumAll v))))

(tc/quick-check 100 testSum)
