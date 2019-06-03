(ns discourje.benchmarks.pipelining
  (require [discourje.core.async :refer :all]
           [discourje.core.logging :refer :all]
           [criterium.core :refer :all])
  ;(:use [discourje.core.async :only [create-protocol generate-infrastructure mep -->> choice >!! <!! <!!! get-label get-content add-infrastructure get-channel msg get-chan]])
  )



(defn discourje-pipeline
  "Pipe-lining protocol generator for Discourje, also has arity for setting the iterations:
  Will start all logic on the main thread

  Generates protocol in line of:

  pipeline-prot
    (mep
      (-->> 1 p0 p1)
      (-->> 1 p1 p2)
      (-->> 1 .. ..)
      (-->> 1 .. pn))
  "
  ([amount]
   (let [protocol (create-protocol (vec (for [p (range amount)] (-->> 1 p (+ p 1)))))
         infra (generate-infrastructure protocol)
         channels (vec (for [p (range amount)] (get-channel p (+ p 1) infra)))
         msg (msg 1 1)]
     (time
       (loop [pipe 0]
         (do
           (>!! (nth channels pipe) msg)
           (<!!! (nth channels pipe) 1)
           (when (true? (< pipe (- amount 1)))
             (recur (+ 1 pipe))))))))
  ([amount iterations]
   (if (<= iterations 1)
     (discourje-pipeline amount)
     (let [protocol (create-protocol (vec (for [p (range amount)] (-->> 1 p (+ p 1)))))
           infras (vec (for [_ (range iterations)] (generate-infrastructure protocol)))
           channels (vec (for [i infras] (vec (for [p (range amount)] (get-channel p (+ p 1) i)))))
           msg (msg 1 1)]
       (time
         (doseq [chans channels]
           (loop [pipe 0]
             (do
               (>!! (nth chans pipe) msg)
               (<!!! (nth chans pipe) 1)
               (when (true? (< pipe (- amount 1)))
                 (recur (+ 1 pipe)))))))
       (doseq [i (range iterations)] (doseq [a (range amount)] (clojure.core.async/close! (get-chan (nth (nth channels i) a)))))))))

;(set-logging-exceptions)
;(discourje-pipeline 2)
;(discourje-pipeline 2)
;(discourje-pipeline 4)
;(discourje-pipeline 8)
;(discourje-pipeline 16)
;(discourje-pipeline 32)
;(discourje-pipeline 64)
;(discourje-pipeline 128)
;(discourje-pipeline 256)

(defn clojure-pipeline
  ([amount]
   (let [channels (vec (for [_ (range amount)] (clojure.core.async/chan 1)))
         msg (msg 1 1)]
     (time
       (loop [pipe 0]
         (do
           (clojure.core.async/>!! (nth channels pipe) msg)
           (clojure.core.async/<!! (nth channels pipe))
           (when (true? (< pipe (- amount 1)))
             (recur (+ 1 pipe))))))))
  ([amount iterations]
   (if (<= iterations 1)
     (clojure-pipeline amount)
     (let [channels (vec (for [_ (range iterations)] (vec (for [_ (range amount)] (clojure.core.async/chan 1)))))
           msg (msg 1 1)]
       (time
         (doseq [chans channels]
           (loop [pipe 0]
             (do
               (clojure.core.async/>!! (nth chans pipe) msg)
               (clojure.core.async/<!! (nth chans pipe))
               (when (true? (< pipe (- amount 1)))
                 (recur (+ 1 pipe)))))))))))

;(clojure-pipeline 2)
;(clojure-pipeline 4)
;(clojure-pipeline 8)
;(clojure-pipeline 16)
;(clojure-pipeline 32)
;(clojure-pipeline 64)
;(clojure-pipeline 128)
;(clojure-pipeline 256)