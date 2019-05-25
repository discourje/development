(ns discourje.benchmarks.parallelism.scatterGather
  (require [discourje.core.async :refer :all]
           [discourje.core.logging :refer :all]
           [criterium.core :refer :all])
  (:use [slingshot.slingshot :only [throw+ try+]]))

(defn discourje-make-work
  "Scatter gather protocol generator for Discourje:
  Will start all workers on separate threads and the master on the main thread in order to make the timing correct.

  Generates protocol in line of:

  worker-prot
    (mep
      (-->> 1 master [worker0 worker1 .. workerN])
      (-->> 1 worker0 master)
      (-->> 1 worker1 master)
      (-->> 1 ....... master)
      (-->> 1 workerN master))

      Starts all participants on separate threads and sends the (msg 1 1) message to all workers and back.
  "
  [workers]
  (let [workers-prot (create-protocol
                       (vec
                         (flatten
                           (flatten
                             (conj
                               [(-->> 1 "m" (vec (for [w (range workers)] (format "w%s" w))))] ;master to workers
                               (vec (for [w (range workers)] (-->> 1 (format "w%s" w) "m")))))))) ; workers to master
        infra (generate-infrastructure workers-prot)
        m->w (vec (for [w (range workers)] (get-channel "m" (format "w%s" w) infra)))
        w->m (vec (for [w (range workers)] {:take (get-channel "m" (format "w%s" w) infra)
                                            :put  (get-channel (format "w%s" w) "m" infra)}))
        msg (msg 1 1)]
    (time
      (do
        (doseq [w w->m]
          (thread
            (do
              (<!!!! (:take w) 1)
              (loop []
                (let [result
                      (try+ (do
                              (>!!! (:put w) msg)
                              true)
                            (catch [:type :incorrect-communication] {}
                              false))]
                  (when (false? result)
                    (recur)))))))
        (>!!! m->w msg)
        (loop [worker-id 0]
          (let [result (try+ (do
                               (<!!! (:put (nth w->m worker-id)) 1)
                               (+ worker-id 1))
                             (catch [:type :incorrect-communication] {}
                               worker-id))]
            (when (true? (< result workers))
              (recur result))))))
    (doseq [mw m->w] (clojure.core.async/close! (get-chan mw)))
    (doseq [wm w->m] (do (clojure.core.async/close! (get-chan (:take wm)))
                         (clojure.core.async/close! (get-chan (:put wm)))))))
(set-logging-exceptions)
(discourje-make-work 2)
(discourje-make-work 4)
(discourje-make-work 8)
(discourje-make-work 16)
(discourje-make-work 32)
(discourje-make-work 64)
(discourje-make-work 128)
(discourje-make-work 256)

(defn clojure-make-work [workers]
  (let [m (clojure.core.async/chan 1)
        workers (vec (for [w (range workers)] (clojure.core.async/chan 1)))]
    (time
      (do
        (doseq [w workers]
          (thread (do
                    (clojure.core.async/<!! w)
                    (clojure.core.async/>!! m 1))))
        (doseq [w workers]
          (do
            (clojure.core.async/>!! w 1)
            (clojure.core.async/<!! m)))))))

(clojure-make-work 2)
(clojure-make-work 4)
(clojure-make-work 8)
(clojure-make-work 16)
(clojure-make-work 32)
(clojure-make-work 64)
(clojure-make-work 128)
(clojure-make-work 265)
