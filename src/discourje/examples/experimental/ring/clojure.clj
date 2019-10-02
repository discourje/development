(ns discourje.examples.experimental.ring.clojure
  (require [clojure.core.async :refer [>!! <!! chan thread]]
           [discourje.examples.experimental.util :refer :all]))

;; Implementation

(load "threads")

;; Run

(def run
  (fn [time k n-iter]
    (let [chans (vec (for [_ (range k)] (chan 1)))
          threads (fn [] (vec (for [i (range k)] (thread-worker [i k] chans n-iter))))]
      (bench time #(join (threads))))))

(run 60 2 1)