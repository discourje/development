(ns discourje.benchmarks.scatterGather
  (require [discourje.core.async :refer :all]
           [discourje.core.logging :refer :all]
           [criterium.core :refer :all])
  (:use [slingshot.slingshot :only [throw+ try+]]))

(defn discourje-scatter-gather
  "Scatter gather protocol generator for Discourje:
  Will start all workers on separate threads and the master on the main thread in order to make the timing correct.
  Also has arity for iteration amount
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
  ([workers]
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
         msg (msg 1 1)
         time (custom-time
                (do
                  (doseq [w w->m]
                    (thread
                      (do
                        (<!!! (:take w) 1)
                        (loop []
                          (let [result
                                (try+ (do
                                        (>!! (:put w) msg)
                                        true)
                                      (catch [:type :incorrect-communication] {}
                                        false))]
                            (when (false? result)
                              (recur)))))))
                  (>!! m->w msg)
                  (loop [worker-id 0]
                    (let [result (do
                                   (<!! (:put (nth w->m worker-id)) 1)
                                   (+ worker-id 1))]
                      (when (true? (< result workers))
                        (recur result))))))]
     (doseq [mw m->w] (clojure.core.async/close! (get-chan mw)))
     (doseq [wm w->m] (do (clojure.core.async/close! (get-chan (:take wm)))
                          (clojure.core.async/close! (get-chan (:put wm)))))
     time))
  ([workers iterations]
   (if (<= iterations 1)
     (discourje-scatter-gather workers)
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
           msg (msg 1 1)
           time (custom-time
                  (doseq [_ (range iterations)]
                          (do
                            (doseq [w w->m]
                              (thread
                                (do
                                  (<!!! (:take w) 1)
                                  (loop []
                                    (let [result
                                          (try+ (do
                                                  (>!! (:put w) msg)
                                                  true)
                                                (catch [:type :incorrect-communication] {}
                                                  false))]
                                      (when (false? result)
                                        (recur)))))))
                            (>!! m->w msg)
                            (loop [worker-id 0]
                              (let [result (do
                                             (<!! (:put (nth w->m worker-id)) 1)
                                             (+ worker-id 1))]
                                (when (true? (< result workers))
                                  (recur result))))
                            (force-monitor-reset! (get-monitor (first m->w))))))]
       time))))
;(set-logging-exceptions)
;(discourje-scatter-gather 2)
;(discourje-scatter-gather 4)
;(discourje-scatter-gather 8)
;(discourje-scatter-gather 16 16)
;(discourje-scatter-gather 2 150000)
;(discourje-scatter-gather 3 70000)
;(discourje-scatter-gather 4 8000)
;(discourje-scatter-gather 6 3000)
;(discourje-scatter-gather 8 1500)
;(discourje-scatter-gather 12 560)
;(discourje-scatter-gather 16 400)
;(discourje-scatter-gather 24 180) FOR 1 MIN!!!
;(discourje-scatter-gather 32 100) FOR 1 MIN!!!
;(discourje-scatter-gather 64)
;(discourje-scatter-gather 128)
;(discourje-scatter-gather 256)

(defn clojure-scatter-gather
  "Scatter gather generator for Clojure:
   Will start all workers on separate threads and the master on the main thread in order to make the timing correct."
  ([workers]
   (let [master-to-workers (vec (for [_ (range workers)] (clojure.core.async/chan 1)))
         workers-to-master (vec (for [_ (range workers)] (clojure.core.async/chan 1)))
         msg (msg 1 1)
         time (custom-time
                (do
                  (doseq [worker-id (range workers)]
                    (thread (do
                              (clojure.core.async/<!! (nth workers-to-master worker-id))
                              (clojure.core.async/>!! (nth master-to-workers worker-id) msg))))
                  (doseq [w workers-to-master] (clojure.core.async/>!! w msg))
                  (loop [worker-id 0]
                    (clojure.core.async/<!! (nth master-to-workers worker-id))
                    (when (true? (< worker-id (- workers 1)))
                      (recur (+ 1 worker-id))))))]
     (doseq [chan (range workers)] (clojure.core.async/close! (nth master-to-workers chan))
                                   (clojure.core.async/close! (nth workers-to-master chan)))
     time))
  ([workers iterations]
   (if (<= iterations 1)
     (clojure-scatter-gather workers)
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
            msg (msg 1 1)
            time (custom-time
                   (doseq [_ (range iterations)]
                     (do
                       (doseq [w w->m]
                         (thread
                           (do
                             (clojure.core.async/<!! (get-chan (:take w)))
                             (loop []
                               (let [result
                                     (try+ (do
                                             (clojure.core.async/>!! (get-chan (:put w)) msg)
                                             true)
                                           (catch [:type :incorrect-communication] {}
                                             false))]
                                 (when (false? result)
                                   (recur)))))))
                       (doseq [w m->w] (clojure.core.async/>!! (get-chan w) msg))
                       (loop [worker-id 0]
                         (let [result (do
                                        (clojure.core.async/<!! (get-chan (:put (nth w->m worker-id))))
                                        (+ worker-id 1))]
                           (when (true? (< result workers))
                             (recur result))))
                       (force-monitor-reset! (get-monitor (first m->w))))))]
        time))))
