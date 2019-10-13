(ns discourje.examples.tacas2020.micro.one_all_one.discourje
  (require [discourje.core.async :refer :all]
           [discourje.examples.tacas2020.main :refer [bench]]))

;;
;; Configuration
;;

(enable-wildcard)
(discourje.core.logging/set-logging-exceptions)
(discourje.core.logging/set-throwing true)
(reset! <!!-unwrap true)

;;
;; Specification
;;

(def master (role "master"))
(def worker (role "worker"))

(def s (dsl :k (fix :X [(ins one-all-one master worker :k Long Long)
                        (fix :X)])))

;;
;; Implementation
;;

(load "threads")

(def run
  (fn [k time n-iter]
    (let [m (moni (spec (ins s k)))
          master->workers (vec (for [i (range k)] (chan 1 master (worker i) m)))
          workers->master (vec (for [i (range k)] (chan 1 (worker i) master m)))
          master-and-workers (fn [] (conj (vec (for [i (range k)] (thread-worker i master->workers workers->master n-iter)))
                                          (thread-master k master->workers workers->master n-iter)))]
      (bench time #(join (master-and-workers))))))

;(run 2 5 1)