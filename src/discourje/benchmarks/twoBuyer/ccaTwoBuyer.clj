(ns discourje.benchmarks.twoBuyer.ccaTwoBuyer
  (:require [clojure.core.async :refer :all]))

(defn buyer1 "order a book from buyer1's perspective"
  ([b1-s s-b1 b1-b2 iteration can-order?]
   (loop [it iteration]
     (do
         (>!! b1-s "book")
         (<!! s-b1)
         (>!! b1-b2 16)
         (<!! s-b1)
         (if (false? @can-order?)
           (println (+ 1 it))
           (recur (+ it 1)))))))

(defn buyer2 "Order a book from buyer2's perspective"
  ([s-b2 b1-b2 b2-s can-order?]
   (loop []
     (do (<!! s-b2)
         (<!! b1-b2)
         (>!! b2-s "ok")
         (>!! b2-s "address")
         (<!! s-b2)
         (when (true? @can-order?) (recur))))))

(defn seller "Order book from seller's perspective"
  ([b1-s s-b1 s-b2 b2-s can-order?]
   (loop []
     (do (<!! b1-s)
         (>!! s-b1 20)
         (>!! s-b2 20)
         (<!! b2-s)
         (<!! b2-s)
         (>!! s-b2 3)
         (>!! s-b1 3)
         (when (true? @can-order?) (recur))))))
(def b1-s (chan 1))
(def s-b1 (chan 1))
(def b1-b2 (chan 1))
(def s-b2 (chan 1))
(def b2-s (chan 1))

;start each participant on another thread
(defn start! [can-order?]
  (let [b1 (thread (buyer1 b1-s s-b1 b1-b2 1 can-order?))
        b2 (thread (buyer2 s-b2 b1-b2 b2-s can-order?))
        s (thread (seller b1-s s-b1 s-b2 b2-s can-order?))]
    (thread
      (do (Thread/sleep 5000)
          (reset! can-order? false)
          (clojure.core.async/close! b1)
          (clojure.core.async/close! b2)
          (clojure.core.async/close! s))
      )))
(dotimes [_ 10] (start! (atom true)))
