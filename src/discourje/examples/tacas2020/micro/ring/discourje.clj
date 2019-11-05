(ns discourje.examples.tacas2020.micro.ring.discourje
  (require [discourje.core.async :refer :all]
           [discourje.examples.tacas2020.main :refer [bench]]))

;;
;; Configuration
;;

(discourje.core.logging/set-logging-exceptions)
(discourje.core.logging/set-throwing true)

;;
;; Specification
;;

(def worker (role "worker"))

(def s (dsl :k (fix :X [(ins ring worker :k Long)
                        (fix :X)])))

;;
;; Implementation
;;

(load "threads")

(def run
  (fn [k time n-iter]
    (let [m (moni (spec (ins s k)))
          workers->workers (vec (for [i (range k)] (chan 1 (worker i) (worker (mod (inc i) k)) m)))
          workers (fn [] (vec (for [i (range k)] (thread-worker i k workers->workers n-iter))))]
      (bench time #(join (workers))))))

;(run 2 5 1)