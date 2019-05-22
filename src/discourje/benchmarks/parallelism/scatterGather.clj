(ns discourje.benchmarks.parallelism.scatterGather
  (require [discourje.core.async :refer :all]
           [discourje.core.logging :refer :all]
           [criterium.core :refer :all])
  (:use [slingshot.slingshot :only [throw+ try+]]))

(def two-Workers
  (mep
    (-->> 1 "m" ["w0" "w1"])
    (-->> 1 "w0" "m")
    (-->> 1 "w1" "m")))
(def infrastructure (generate-infrastructure two-Workers))
(def m-w1 (get-channel "m" "w0" infrastructure))
(def w1-m (get-channel "w1" "m" infrastructure))
(def w2-m (get-channel "w2" "m" infrastructure))
(println two-Workers)
(defn make-work
  "scatter gather protocol generator

  Generates protocol in line of:r

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
        (thread (>!! m->w message))
        (doseq [w w->m]
          (thread
            (do
              (<!! (:take w) 1)
              (loop []
                (if-let [result (try+ (>!! (:put w) message)
                               (catch [:type :incorrect-communication] {}))]
                  (recur)))
              ;(>!! (:put w) message)
              )))))))

(defn try-times*
  "Executes thunk. If an exception is thrown, will retry. At most n retries
  are done. If still some exception is thrown it is bubbled upwards in
  the call chain."
  [n thunk]
  (loop [n n]
    (if-let [result (try
                      [(thunk)]
                      (catch Exception e
                        (when (zero? n)
                          (throw e))))]
      (result 0)
      (recur (dec n)))))
(make-work 2)
