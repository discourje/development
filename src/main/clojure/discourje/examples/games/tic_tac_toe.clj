(ns discourje.examples.games.tic-tac-toe
  (:require [clojure.core.async]
            [discourje.core.async]
            [discourje.core.spec :as s]
            [discourje.core.lint :as l]
            [discourje.examples.config :as config]))

;;;;;
;;;;; Specification
;;;;;

(s/defrole ::alice)
(s/defrole ::bob)

(s/defsession ::tic-tac-toe []
  (s/alt (::tic-tac-toe-turn ::alice ::bob)
         (::tic-tac-toe-turn ::bob ::alice)))

(s/defsession ::tic-tac-toe-turn [r1 r2]
  (s/--> Long r1 r2)
  (s/alt (::tic-tac-toe-turn r2 r1)
         (s/par (s/close r1 r2)
                (s/close r2 r1))))

(defn spec [] (tic-tac-toe))

(when (some? config/*lint*)
  (set! config/*output* (l/lint (spec))))

;;;;;
;;;;; Implementation
;;;;;

(config/clj-or-dcj)

(def blank " ")
(def cross "x")
(def nought "o")

(def initial-grid
  [blank blank blank
   blank blank blank
   blank blank blank])

(defn get-blank [g]
  (loop [i (long (rand-int 9))]
    (if (= (nth g i) blank)
      i
      (recur (mod (inc i) 9)))))

(defn put [g i x-or-o]
  (assoc g i x-or-o))

(defn not-final? [g]
  (and (loop [i 0]
         (cond (= (nth g i) blank) true
               (= i 8) false
               :else (recur (inc i))))
       (every? #(= false %) (for [l [(set [(nth g 0) (nth g 1) (nth g 2)])
                                     (set [(nth g 3) (nth g 4) (nth g 5)])
                                     (set [(nth g 6) (nth g 7) (nth g 8)])
                                     (set [(nth g 0) (nth g 3) (nth g 6)])
                                     (set [(nth g 1) (nth g 4) (nth g 7)])
                                     (set [(nth g 2) (nth g 5) (nth g 8)])
                                     (set [(nth g 0) (nth g 4) (nth g 8)])
                                     (set [(nth g 2) (nth g 4) (nth g 6)])]]
                              (and (= (count l) 1) (not= (first l) blank))))))

(defn println-grid [g]
  (println)
  (println "+---+---+---+")
  (println "|" (nth g 0) "|" (nth g 1) "|" (nth g 2) "|")
  (println "+---+---+---+")
  (println "|" (nth g 3) "|" (nth g 4) "|" (nth g 5) "|")
  (println "+---+---+---+")
  (println "|" (nth g 6) "|" (nth g 7) "|" (nth g 8) "|")
  (println "+---+---+---+")
  (println))

(when (some? config/*run*)
  (let [input config/*input*]

    (let [;; Create channels
          a->b (a/chan)
          b->a (a/chan)

          ;; Link monitor [optional]
          _
          (if (= config/*run* :dcj)
            (let [m (a/monitor (spec) :n 2)]
              (a/link a->b alice bob m)
              (a/link b->a bob alice m)))

          ;; Spawn threads
          alice
          (a/thread (loop [g initial-grid]
                      (let [i (get-blank g)
                            g (put g i cross)]
                        (a/>!! a->b i)
                        (if (not-final? g)
                          (let [i (a/<!! b->a)
                                g (put g i nought)]
                            (if (not-final? g)
                              (recur g)))
                          (println-grid g))))
                    (a/close! a->b))

          bob
          (a/thread (loop [g initial-grid]
                      (let [i (a/<!! a->b)
                            g (put g i cross)]
                        (if (not-final? g)
                          (let [i (get-blank g)
                                g (put g i nought)]
                            (a/>!! b->a i)
                            (if (not-final? g)
                              (recur g)
                              (println-grid g))))))
                    (a/close! b->a))

          ;; Await termination
          output
          (do (a/<!! alice)
              (a/<!! bob)
              nil)]

      (set! config/*output* output))))