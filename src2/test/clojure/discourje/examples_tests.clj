(ns discourje.examples-tests
  (:refer-clojure :exclude [print])
  (:require [clojure.test :refer :all]
            [clojure.pprint :as p]
            [discourje.examples.main :as main]))

(defn- print [x]
  (prn)
  (p/pprint x))

(deftest micro-tests

  ;; Ring

  (print (main/run-all [:clj :dcj]
                       ['discourje.examples.micro.ring]
                       {:buffered [true] :k [2] :secs [0]}))
  (is true)

  (print (main/run-all [:clj :dcj]
                       ['discourje.examples.micro.ring]
                       {:buffered [false] :k [2] :secs [0]}))
  (is true)

  ;; Mesh

  (print (main/run-all [:clj :dcj]
                       ['discourje.examples.micro.mesh]
                       {:buffered [true] :k [2] :secs [0]}))
  (is true)

  (print (main/run-all [:clj :dcj]
                       ['discourje.examples.micro.mesh]
                       {:buffered [false] :k [2] :secs [0]}))
  (is true)

  ;; Star

  (print (main/run-all [:clj :dcj]
                       ['discourje.examples.micro.star]
                       {:buffered [true] :ordered-sends [true] :ordered-receives [true] :k [2] :secs [0]}))
  (is true)

  (print (main/run-all [:clj :dcj]
                       ['discourje.examples.micro.star]
                       {:buffered [true] :ordered-sends [true] :ordered-receives [false] :k [2] :secs [0]}))
  (is true)

  (print (main/run-all [:clj :dcj]
                       ['discourje.examples.micro.star]
                       {:buffered [true] :ordered-sends [false] :ordered-receives [true] :k [2] :secs [0]}))
  (is true)

  (print (main/run-all [:clj :dcj]
                       ['discourje.examples.micro.star]
                       {:buffered [true] :ordered-sends [false] :ordered-receives [false] :k [2] :secs [0]}))
  (is true)

  (print (main/run-all [:clj :dcj]
                       ['discourje.examples.micro.star]
                       {:buffered [false] :ordered-sends [true] :k [2] :secs [0]}))
  (is true)

  (print (main/run-all [:clj :dcj]
                       ['discourje.examples.micro.star]
                       {:buffered [false] :ordered-sends [false] :k [2] :secs [0]}))
  (is true))

(deftest games-tests

  ;; Tic-Tac-Toe

  (print (main/run-all [:clj :dcj]
                       ['discourje.examples.games.tic-tac-toe]
                       {}))
  (is true)

  ;; Rock-Paper-Scissors

  (print (main/run-all [:clj :dcj]
                       ['discourje.examples.games.rock-paper-scissors]
                       {:k [3]}))
  (is true)

  ;; Go Fish

  (print (main/run-all [:clj :dcj]
                       ['discourje.examples.games.go-fish]
                       {:k [3]}))
  (is true))

(deftest npb3-tests

  (print (main/run-all [:clj :dcj]
                       ['discourje.examples.npb3.cg]
                       {:k [3] :class ['w] :secs [0]}))
  (is true)

  (print (main/run-all [:clj :dcj]
                       ['discourje.examples.npb3.ft]
                       {:k [3] :class ['w] :secs [0]}))
  (is true)

  (print (main/run-all [:clj :dcj]
                       ['discourje.examples.npb3.is]
                       {:k [3] :class ['w] :secs [0]}))
  (is true)

  (print (main/run-all [:clj :dcj]
                       ['discourje.examples.npb3.cg]
                       {:k [3] :class ['w] :secs [0]}))
  (is true))