(ns discourje.examples.tacas2020.micro.ring.clojure
  (require [clojure.core.async :refer [>!! <!! close! chan thread]]
           [discourje.examples.tacas2020.main :refer [bench]]))

;; Implementation

(load "threads")

;; Run

(def run
  (fn [k time n-iter]
    (let [chans (vec (for [_ (range k)] (chan 1)))
          threads (fn [] (vec (for [i (range k)] (thread-worker [i k] chans n-iter))))]
      (bench time #(join (threads))))))

;(run 2 60 1)