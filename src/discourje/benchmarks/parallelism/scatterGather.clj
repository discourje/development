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
        (thread (>!!! m->w msg))
        (loop [index 0]
          (let [w (nth w->m index)]
            (do  (thread
              (do
                (println "taking from " (get-provider (:take w)) " for " (get-consumer (:take w)))
                (<!!!! (:take w) 1)
                (loop []
                  (let [result (try+ (do (println "putting on " (get-consumer (:put w)))
                                         (>!!! (:put w) msg) true)
                                     (catch [:type :incorrect-communication] {:keys [type message]}
                                       (do (println type message)
                                         false)))]
                    (when (false? result)
                      (recur))))))
                 (println "thread started"))
            (println "done do")
          (when (true? (< index (- workers 1)))
            (do (println index" going to another worker " (+ 1 index))
            (recur (+ 1 index))))))
        (loop [worker-id 0]
          (println worker-id)
          (let [result (try+ (do
                               (println "master taking from " worker-id)
                               (<!!! (:put (nth w->m worker-id)) 1) (+ worker-id 1))
                             (catch [:type :incorrect-communication] {}
                               (do
                                 (println "master exception")
                                 worker-id)))]
            (do (println "result is " result)
                (when (true? (< result workers))
                  (recur result)))))
        (println "Done")))
    (doseq [mw m->w] (clojure.core.async/close! (get-chan mw)))
    (doseq [wm w->m] (do (clojure.core.async/close! (get-chan (:take wm)))
                         (clojure.core.async/close! (get-chan (:put wm)))))))
(set-logging)
(make-work 5)
