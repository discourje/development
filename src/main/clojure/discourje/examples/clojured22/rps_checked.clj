(ns discourje.examples.clojured22.rps-checked
  (:require [clojure.test :refer [deftest]]
            [discourje.core.async :refer [thread chan >!! <!! alts!! monitor]]
            [discourje.core.spec :as s]
            [discourje.examples.clojured22.rps :refer :all]))

(s/defsession :rps []
  (s/cat
    (s/--> "alice" "judge")
    (s/--> "bob" "judge")
    (s/--> "judge" "main")))

(def m (monitor :rps :n 4))

(defn rand-player [name]
  (let [out (chan name "judge" m {})]
    (thread
      (>!! out [name (rand-nth MOVES)]))
    out))

(defn judge [p1 p2]
  (let [out (chan "judge" "main" m {})]
    (thread
      (let [m1 (<!! p1)
            m2 (<!! p2)]
        (>!! out (winner m1 m2))))
    out))

(deftest rps-checked-good
  (let [x (rand-player "alice")
        y (rand-player "bob")
        z (judge x y)]
    (report (<!! z))))

(deftest rps-checked-bad1a
  (let [x (rand-player "alice")
        y (rand-player "bob")
        z (judge x y)]
    (report (<!! y))))

(deftest rps-checked-bad1b
  (let [x (rand-player "alice")
        y (rand-player "bob")
        z (judge x y)]
    (Thread/sleep 500)
    (report (<!! y))))

(deftest rps-checked-bad2

  (defn judge [p1 p2]
    (let [out (chan "judge" "main" m {})]
      (thread
        (let [m (<!! p1)
              [name _] m]
          (>!! out name)))
      out))

  (let [x (rand-player "alice")
        y (rand-player "bob")
        z (judge x y)]
    (report (<!! z))))