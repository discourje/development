(ns discourje.benchmarks.scatterGather
  (require [discourje.core.async :refer :all]
           [discourje.core.logging :refer :all]
           [criterium.core :refer :all]))

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
         m->w (vec (for [w (range workers)] (get-channel infra "m" (format "w%s" w) )))
         w->m (vec (for [w (range workers)] {:take (get-channel infra "m" (format "w%s" w))
                                             :put  (get-channel infra (format "w%s" w) "m")}))
         msg (msg 1 1)
         time (custom-time
                (do
                  (doseq [w w->m]
                    (thread
                      (do
                        (<!!! (:take w) 1)
                        (loop []
                          (when (nil? (>!! (:put w) msg))
                            (recur))))))
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
           m->w (vec (for [w (range workers)] (get-channel infra "m" (format "w%s" w))))
           w->m (vec (for [w (range workers)] {:take (get-channel infra "m" (format "w%s" w))
                                               :put  (get-channel infra (format "w%s" w) "m")}))
           msg (msg 1 1)
           time (custom-time
                  (doseq [_ (range iterations)]
                    (do
                      (doseq [w w->m]
                        (thread
                          (do
                            (<!!! (:take w) 1)
                            (loop []
                              (when (nil? (>!! (:put w) msg))
                                (recur))))))
                      (>!! m->w msg)
                      (loop [worker-id 0]
                        (let [result (do
                                       (<!! (:put (nth w->m worker-id)) 1)
                                       (+ worker-id 1))]
                          (when (< result workers)
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
;(discourje-scatter-gather 24 180) ;FOR 1 MIN!!!
;(discourje-scatter-gather 32 100) ;FOR 1 MIN!!!
;(discourje-scatter-gather 64)
;(discourje-scatter-gather 128)
;(discourje-scatter-gather 256)

(defn discourje-scatter-gather-parallel
  "Scatter gather protocol generator for Discourje:
  Will start all workers on separate threads and the master on the main thread in order to make the timing correct.
  Also has arity for iteration amount
  Generates protocol in line of:

  worker-prot
    (mep
      (-->> 1 master [worker0 worker1 .. workerN])
      (par [(-->> 1 worker0 master)]
           [(-->> 1 worker1 master)]
           [(-->> 1 ....... master)]
           [(-->> 1 workerN master)]))

      Starts all participants on separate threads and sends the (msg 1 1) message to all workers and back.
  "
  ([workers]
   (let [workers-prot (create-protocol
                        (vec
                          (flatten
                            (flatten
                              (conj
                                [(-->> 1 "m" (vec (for [w (range workers)] (format "w%s" w))))] ;master to workers
                                (make-parallel (vec (for [w (range workers)] [(-->> 1 (format "w%s" w) "m")])))))))) ; workers to master
         infra (generate-infrastructure workers-prot)
         m->w (vec (for [w (range workers)] (get-channel infra"m" (format "w%s" w))))
         w->m (vec (for [w (range workers)] {:take (get-channel infra"m" (format "w%s" w))
                                             :put  (get-channel infra (format "w%s" w) "m")}))
         msg (msg 1 1)
         time (custom-time
                (do
                  (doseq [w w->m]
                    (thread
                      (do
                        (<!!! (:take w) 1)
                        (>!! (:put w) msg))))
                  (>!! m->w msg)
                  (loop [worker-id 0]
                    (let [result (do
                                   (<!! (:put (nth w->m worker-id)) 1)
                                   (+ worker-id 1))]
                      (when (true? (< result workers))
                        (recur result))))))]
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
                                  (make-parallel (vec (for [w (range workers)] [(-->> 1 (format "w%s" w) "m")])))))))) ; workers to master
           infra (generate-infrastructure workers-prot)
           m->w (vec (for [w (range workers)] (get-channel infra"m" (format "w%s" w))))
           w->m (vec (for [w (range workers)] {:take (get-channel infra"m" (format "w%s" w))
                                               :put  (get-channel infra (format "w%s" w) "m")}))
           msg (msg 1 1)
           time (custom-time
                  (doseq [_ (range iterations)]
                    (do
                      (doseq [w w->m]
                        (thread
                          (do
                            (<!!! (:take w) 1)
                            (>!! (:put w) msg))))
                      (>!! m->w msg)
                      (loop [worker-id 0]
                        (let [result (do
                                       (<!! (:put (nth w->m worker-id)) 1)
                                       (+ worker-id 1))]
                          (when (< result workers)
                            (recur result))))
                      (force-monitor-reset! (get-monitor (first m->w))))))]
       time))))

(defn clojure-scatter-gather
  ([workers]
   (let [workers-prot (create-protocol
                        (vec
                          (flatten
                            (flatten
                              (conj
                                [(-->> 1 "m" (vec (for [w (range workers)] (format "w%s" w))))] ;master to workers
                                (vec (for [w (range workers)] (-->> 1 (format "w%s" w) "m")))))))) ; workers to master
         infra (generate-infrastructure workers-prot)
         m->w (vec (for [w (range workers)] (get-channel infra "m" (format "w%s" w))))
         w->m (vec (for [w (range workers)] {:take (get-channel infra "m" (format "w%s" w))
                                             :put  (get-channel infra (format "w%s" w) "m")}))
         msg (msg 1 1)
         time (custom-time
                (do
                  (doseq [w w->m]
                    (thread
                      (do
                        (clojure.core.async/<!! (get-chan (:take w)))
                        (loop []
                          (when (nil? (clojure.core.async/>!! (get-chan (:put w)) msg))
                            (recur))))))
                  (doseq [w m->w] (clojure.core.async/>!! (get-chan w) msg))
                  (loop [worker-id 0]
                    (let [result (do
                                   (clojure.core.async/<!! (get-chan (:put (nth w->m worker-id))))
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
           m->w (vec (for [w (range workers)] (get-channel infra "m" (format "w%s" w))))
           w->m (vec (for [w (range workers)] {:take (get-channel infra "m" (format "w%s" w))
                                               :put  (get-channel infra (format "w%s" w) "m")}))
           msg (msg 1 1)
           time (custom-time
                  (doseq [_ (range iterations)]
                    (do
                      (doseq [w m->w] (clojure.core.async/>!! (get-chan w) msg))
                      (doseq [w w->m]
                        (thread
                          (do
                            (clojure.core.async/<!! (get-chan (:take w)))
                            (loop []
                              (when (nil? (clojure.core.async/>!! (get-chan (:put w)) msg))
                                (recur))))))
                      (loop [worker-id 0]
                        (let [result (do
                                       (clojure.core.async/<!! (get-chan (:put (nth w->m worker-id))))
                                       (+ worker-id 1))]
                          (when (< result workers)
                            (recur result))))
                      (force-monitor-reset! (get-monitor (first m->w))))))]
       time))))

;(clojure-scatter-gather 16 16)
