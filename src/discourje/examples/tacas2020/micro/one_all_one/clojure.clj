(ns discourje.examples.tacas2020.micro.one_all_one.clojure
  (:require [clojure.core.async :refer [>!! <!! close! chan thread]]
           [discourje.examples.tacas2020.main :refer [bench]]))

;;
;; Implementation
;;

(load "threads")

(def run
  (fn [k time n-iter]
    (let [master->workers (vec (for [i (range k)] (chan 1)))
          workers->master (vec (for [i (range k)] (chan 1)))
          master-and-workers (fn [] (conj (vec (for [i (range k)] (thread-worker i master->workers workers->master n-iter)))
                                          (thread-master k master->workers workers->master n-iter)))]
      (bench time #(join (master-and-workers))))))

;(run 2 5 1)