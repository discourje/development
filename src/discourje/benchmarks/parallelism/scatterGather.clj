(ns discourje.benchmarks.parallelism.scatterGather
  (require [discourje.core.async :refer :all]
           [discourje.core.logging :refer :all]
           [criterium.core :refer :all])
  (:use [slingshot.slingshot :only [throw+ try+]]))

(defn make-work
  "scatter gather protocol generator

  Generates protocol in line of:

  worker-prot
    (mep
      (-->> 1 m [w0 w1 .. wN])
      (-->> 1 w0 m)
      (-->> 1 w1 m)
      (-->> 1 .. m)
      (-->> 1 wN m))

      Starts all participants on separate threads and sends the (msg 1 1) message to all workers and back.
  "
  [workers]
  (let [workers-prot (create-protocol
                       (vec (flatten (flatten
                                       (conj
                                         [(-->> 1 "m" (vec (for [w (range workers)] (format "w%s" w))))] ;master to workers
                                         (vec (for [w (range workers)] (-->> 1 (format "w%s" w) "m")))))))) ; workers to master
        infra (generate-infrastructure workers-prot)
        m->w (vec (for [w (range workers)] (get-channel "m" (format "w%s" w) infra)))
        w->m (vec (for [w (range workers)] {:take (get-channel "m" (format "w%s" w) infra)
                                            :put  (get-channel (format "w%s" w) "m" infra)}))
        message (msg 1 1)]
    (time
      (do
        (thread (>!!! m->w message))
        (doseq [w w->m]
          (thread
            (do
              (<!!!! (:take w) 1)
              (loop []
                (let [result (try+ (>!!! (:put w) message) true
                                   (catch [:type :incorrect-communication] {}
                                     false))]
                  (when (false? result)
                    (recur)))))))
        (loop [worker-id 0]
          (println worker-id)
          (let [result (try+ (<!!!!(:put (nth w->m worker-id)) 1) (+ worker-id 1)
                             (catch [:type :incorrect-communication] {}
                               worker-id))]
            (when (not (== result (- workers 1)))
              (recur result))))))))
(set-logging-exceptions)
(make-work 10)
