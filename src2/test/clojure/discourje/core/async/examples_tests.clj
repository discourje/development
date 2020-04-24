(ns discourje.core.async.examples-tests
  (:require [clojure.test :refer :all]
            [clojure.pprint :as p]
            [discourje.core.async.examples :as e]))

(defn- print [x]
  (prn)
  (p/pprint x))

(deftest micro-tests

  ;; Ring

  (print (e/compare 'discourje.core.async.examples.micro.ring
                    [:clj :dcj]
                    {:buffered true :k 2 :secs 1}))
  (is true)

  (print (e/compare 'discourje.core.async.examples.micro.ring
                    [:clj :dcj]
                    {:buffered false :k 2 :secs 1}))
  (is true)

  ;; Mesh

  (print (e/compare 'discourje.core.async.examples.micro.mesh
                    [:clj :dcj]
                    {:buffered true :k 2 :secs 1}))
  (is true)

  (print (e/compare 'discourje.core.async.examples.micro.mesh
                    [:clj :dcj]
                    {:buffered false :k 2 :secs 1}))
  (is true)

  ;; Star

  (print (e/compare 'discourje.core.async.examples.micro.star
                    [:clj :dcj]
                    {:buffered true :ordered-sends true :ordered-receives true :k 2 :secs 1}))
  (is true)

  (print (e/compare 'discourje.core.async.examples.micro.star
                    [:clj :dcj]
                    {:buffered true :ordered-sends true :ordered-receives false :k 2 :secs 1}))
  (is true)

  (print (e/compare 'discourje.core.async.examples.micro.star
                    [:clj :dcj]
                    {:buffered true :ordered-sends false :ordered-receives true :k 2 :secs 1}))
  (is true)

  (print (e/compare 'discourje.core.async.examples.micro.star
                    [:clj :dcj]
                    {:buffered true :ordered-sends false :ordered-receives false :k 2 :secs 1}))
  (is true)

  (print (e/compare 'discourje.core.async.examples.micro.star
                    [:clj :dcj]
                    {:buffered false :ordered-sends true :k 2 :secs 1}))
  (is true)

  (print (e/compare 'discourje.core.async.examples.micro.star
                    [:clj :dcj]
                    {:buffered false :ordered-sends false :k 2 :secs 1}))
  (is true))