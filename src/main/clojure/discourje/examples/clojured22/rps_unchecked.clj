(ns discourje.examples.clojured22.rps-unchecked
  (:require [clojure.test :refer [deftest]]
            [clojure.core.async :refer [thread chan >!! <!! alts!!]]
            [discourje.examples.clojured22.rps :refer :all]))

(defn rand-player [name]
  (let [out (chan)]
    (thread
      (>!! out [name (rand-nth MOVES)]))
    out))

(defn judge [p1 p2]
  (let [out (chan)]
    (thread
      (let [m1 (<!! p1)
            m2 (<!! p2)]
        (>!! out (winner m1 m2))))
    out))

(deftest rps-unchecked-good
  (let [x (rand-player "alice")
        y (rand-player "bob")
        z (judge x y)]
    (report (<!! z))))

(deftest rps-unchecked-bad1a
  (let [x (rand-player "alice")
        y (rand-player "bob")
        z (judge x y)]
    (report (<!! y))))

(deftest rps-unchecked-bad1b
  (let [x (rand-player "alice")
        y (rand-player "bob")
        z (judge x y)]
    (Thread/sleep 500)
    (report (<!! y))))

(deftest rps-unchecked-bad2

  (defn judge [p1 p2]
    (let [out (chan)]
      (thread
        (let [m (<!! p1)
              [name _] m]
          (>!! out name)))
      out))

  (let [x (rand-player "alice")
        y (rand-player "bob")
        z (judge x y)]
    (report (<!! z))))