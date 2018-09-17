(ns discourje.core-test
  (:require [clojure.test :refer :all]
            [discourje.core :refer :all])
  (:refer-clojure :exclude [send])
  (:import (discourje.core message)))

(deftest messageTest
  (let [m (message. "hello")]
    (is (= (str "hello") (:data m)))))