(ns discourje.core.async.examples-tests
  (:refer-clojure :exclude [print])
  (:require [clojure.test :refer :all]
            [clojure.pprint :as p]
            [discourje.core.async.examples :as e]))

(defn- print [x]
  (prn)
  (p/pprint x))

(deftest micro-tests

  ;; Ring

  (print (e/run-all [:clj :dcj]
                    ['discourje.core.async.examples.micro.ring]
                    {:buffered [true] :k [2] :secs [1]}))
  (is true)

  (print (e/run-all [:clj :dcj]
                    ['discourje.core.async.examples.micro.ring]
                    {:buffered [false] :k [2] :secs [1]}))
  (is true)

  ;; Mesh

  (print (e/run-all [:clj :dcj]
                    ['discourje.core.async.examples.micro.mesh]
                    {:buffered [true] :k [2] :secs [1]}))
  (is true)

  (print (e/run-all [:clj :dcj]
                    ['discourje.core.async.examples.micro.mesh]
                    {:buffered [false] :k [2] :secs [1]}))
  (is true)

  ;; Star

  (print (e/run-all [:clj :dcj]
                    ['discourje.core.async.examples.micro.star]
                    {:buffered [true] :ordered-sends [true] :ordered-receives [true] :k [2] :secs [1]}))
  (is true)

  (print (e/run-all [:clj :dcj]
                    ['discourje.core.async.examples.micro.star]
                    {:buffered [true] :ordered-sends [true] :ordered-receives [false] :k [2] :secs [1]}))
  (is true)

  (print (e/run-all [:clj :dcj]
                    ['discourje.core.async.examples.micro.star]
                    {:buffered [true] :ordered-sends [false] :ordered-receives [true] :k [2] :secs [1]}))
  (is true)

  (print (e/run-all [:clj :dcj]
                    ['discourje.core.async.examples.micro.star]
                    {:buffered [true] :ordered-sends [false] :ordered-receives [false] :k [2] :secs [1]}))
  (is true)

  (print (e/run-all [:clj :dcj]
                    ['discourje.core.async.examples.micro.star]
                    {:buffered [false] :ordered-sends [true] :k [2] :secs [1]}))
  (is true)

  (print (e/run-all [:clj :dcj]
                    ['discourje.core.async.examples.micro.star]
                    {:buffered [false] :ordered-sends [false] :k [2] :secs [1]}))
  (is true))