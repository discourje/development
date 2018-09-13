(ns projectSetup.testCheckConfig
  (:require [clojure.test :refer :all])
  (:require [projectSetup.testCheck :refer :all])
  (:require [clojure.test.check.generators :as gen])
  (:require [clojure.test.check.properties :as prop])
  (:require [clojure.test.check :as tc]))


(def testSum
  "test summall function for all overloads (arity)"
  (prop/for-all [v (gen/vector gen/int)]
                (== (reduce + v) (apply sumAll v))))

(tc/quick-check 100 testSum)

(def testMessenger
  "test the messenger function, taking in consideration the nil case"
  (prop/for-all [v (gen/vector gen/string)]
    (if (empty? v)
      (= (str "Hello world!") (apply messenger v))
      (= (apply str v) (apply messenger v)))))

(tc/quick-check 100 testMessenger)

(def property
  "always returns a sorted list (this comes from test.check docs) "
  (prop/for-all [v (gen/vector gen/int)]
                (let [s (sort v)]
                  (and (= (count v) (count s))
                       (or (empty? s)
                           (apply <= s))))))

(tc/quick-check 100 property)
