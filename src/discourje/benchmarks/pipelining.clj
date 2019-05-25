(ns discourje.benchmarks.pipelining
  (require [discourje.core.async :refer :all]
           [discourje.core.logging :refer :all]
           [criterium.core :refer :all])
  (:use [slingshot.slingshot :only [throw+ try+]]))


(defn discourje-pipeline [amount]
  (let [protocol (create-protocol (vec (for [p (range amount)] (-->> 1 p (+ p 1)))))
        infra (generate-infrastructure protocol)
        channels (vec (for [p (range amount)] (get-channel p (+ p 1) infra)))
        msg (msg 1 1 )]
    (time
      (loop [pipe 0]
        (do
          (>!!! (nth channels pipe) msg)
          (<!!! (nth channels pipe) 1)
          (when (true? (< pipe (- amount 1)))
            (recur (+ 1 pipe))))))
      ))

(discourje-pipeline 3)