(ns discourje.benchmarks.parallelism.scatterGather
  (require [discourje.core.async :refer :all]
           [discourje.core.logging :refer :all]
           [criterium.core :refer :all]))

(def two-Workers
  (mep
    (-->> 1 "m" ["w0" "w1"])
    (-->> 1 "w0" "m")
    (-->> 1 "w1" "m")))
(def message (msg 1 1))
(def infra (generate-infrastructure two-Workers))
(def m-w1 (get-channel "m" "w0" infra))
(def w1-m (get-channel "w1" "m" infra))
(def w2-m (get-channel "w2" "m" infra))

(defn make-work [workers]
  (let [m->w (vec (for [w (range workers)] (get-channel "m" (format "w%s" w) infra)))
        w->m (vec (for [w (range workers)] {:take (get-channel "m" (format "w%s" w) infra) :put (get-channel (format "w%s" w) "m" infra)}))]
    (time
      (do
        (thread (>!! m->w message))
        (doseq [w w->m]
          (thread
            (do
              (<!! (:take w) 1)
              (>!! (:put w) message))))))))
(make-work 2)
