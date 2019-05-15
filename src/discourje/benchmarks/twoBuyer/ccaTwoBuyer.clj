(ns discourje.benchmarks.twoBuyer.ccaTwoBuyer
  (:require [clojure.core.async :refer :all]))

(defn buyer1 "order a book from buyer1's perspective"
  ([b1-s s-b1 b1-b2 iteration]
   (println iteration)
   (>!! b1-s "book")
   (do (<!! s-b1 )
       (>!! b1-b2 16)
       (when (<!! s-b1)
         (buyer1 b1-s s-b1 b1-b2 (+ iteration 1))))))

(defn buyer2 "Order a book from buyer2's perspective"
  ([s-b2 b1-b2 b2-s]
   (do (<!! s-b2)
       (<!! b1-b2)
       (>!! b2-s "ok")
       (>!! b2-s "address")
       (<!! s-b2)
       (buyer2 s-b2 b1-b2 b2-s))))

(defn seller "Order book from seller's perspective"
  ([b1-s s-b1 s-b2 b2-s]
   (do (<!! b1-s)
       (>!! [s-b1 s-b2] 20)
       (<!! b2-s)
       (<!! b2-s)
       (>!! s-b2 3)
       (>!! s-b1 3)
       (seller b1-s s-b1 s-b2 b2-s))))
(def b1-s (chan 1))
(def s-b1 (chan 1))
(def b1-b2 (chan 1))
(def s-b2 (chan 1))
(def b2-s (chan 1))

;start each participant on another thread
(defn start! []
  (do (thread (buyer1 b1-s s-b1 b1-b2 1))
      (thread (buyer2 s-b2 b1-b2 b2-s))
      (thread (seller b1-s s-b1 s-b2 b2-s))))
(start!)

