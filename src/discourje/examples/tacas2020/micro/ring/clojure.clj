(ns discourje.examples.tacas2020.micro.ring.clojure
  (:require [clojure.core.async :refer [>!! <!! close! chan thread]]
           [discourje.examples.tacas2020.main :refer [bench]]))

;;
;; Implementation
;;

(load "threads")

(def run
  (fn [k time n-iter]
    (let [workers->workers (vec (for [_ (range k)] (chan 1)))
          workers (fn [] (vec (for [i (range k)] (thread-worker i k workers->workers n-iter))))]
      (bench time #(join (workers))))))

;(run 2 5 1)