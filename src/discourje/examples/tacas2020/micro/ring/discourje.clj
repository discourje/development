(ns discourje.examples.tacas2020.micro.ring.discourje
  (require [discourje.core.async :refer :all]
           [discourje.examples.tacas2020.main :refer [bench]]))

;;
;; Configuration
;;

(enable-wildcard)
(discourje.core.logging/set-logging-exceptions)
(discourje.core.logging/set-throwing true)
(reset! <!!-unwrap true)

;; Specification

(def worker (role "worker"))

(def s (dsl :k (fix :X [(insert ring worker :k Long)
                        (fix :X)])))

;; Implementation

(load "threads")

;; Run

(def run
  (fn [k time n-iter]
    (let [m (moni (spec (insert s k)))
          chans (vec (for [i (range k)] (chan 1 (worker i) (worker (mod (inc i) k)) m)))
          threads (fn [] (vec (for [i (range k)] (thread-worker [i k] chans n-iter))))]
      (bench time #(join (threads))))))

;(run 2 5 1)