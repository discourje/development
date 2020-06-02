(ns discourje.core.async.examples.games.tic-tac-toe
  (:require [clojure.core.async]
            [discourje.core.async]
            [discourje.core.async.examples.config :as config]
            [discourje.core.spec :as s]))

(config/clj-or-dcj)

;;;;
;;;; Specification
;;;;

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

;;;;
;;;; Implementation
;;;;

(def blank " ")
(def cross "x")
(def nought "o")

(def initial-grid
  [blank blank blank
   blank blank blank
   blank blank blank])

(def get-blank
  (fn [g]
    (loop [i (long (rand-int 9))]
      (if (= (nth g i) blank)
        i
        (recur (mod (inc i) 9))))))

(def put
  (fn [g i x-or-o]
    (try (assoc g i x-or-o)
         (catch Exception e (println g i x-or-o) (.printStackTrace e)))))

(def not-final?
  (fn [g]
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
                                (and (= (count l) 1) (not= (first l) blank)))))))

(def println-grid
  (fn [g]
    (println)
    (println "+---+---+---+")
    (println "|" (nth g 0) "|" (nth g 1) "|" (nth g 2) "|")
    (println "+---+---+---+")
    (println "|" (nth g 3) "|" (nth g 4) "|" (nth g 5) "|")
    (println "+---+---+---+")
    (println "|" (nth g 6) "|" (nth g 7) "|" (nth g 8) "|")
    (println "+---+---+---+")
    (println)))

(let [input config/*input*
      _ (:resolution input)]

  (let [;; Start timer
        begin (System/nanoTime)

        ;; Create channels
        a->b (a/chan)
        b->a (a/chan)

        ;; Link monitor [optional]
        _
        (if (= config/*lib* :dcj)
          (let [s (s/session ::tic-tac-toe [])
                m (a/monitor s)]
            (a/link a->b (s/role ::alice) (s/role ::bob) m)
            (a/link b->a (s/role ::bob) (s/role ::alice) m)))

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
            nil)

        ;; Stop timer
        end (System/nanoTime)]

    (set! config/*output* output)
    (set! config/*time* (- end begin))))