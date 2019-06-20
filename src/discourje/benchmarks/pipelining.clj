(ns discourje.benchmarks.pipelining
  (require [discourje.core.async :refer :all]
           [discourje.core.logging :refer :all]))

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
         msg (msg 1 1)
         time (custom-time
                (loop [pipe 0]
                  (do
                    (>!! (nth channels pipe) msg)
                    (<!!! (nth channels pipe) 1)
                    (when (true? (< pipe (- amount 1)))
                      (recur (+ 1 pipe))))))]
     (close-infrastructure! infra)
     time))
  ([amount iterations]
   (if (<= iterations 1)
     (discourje-pipeline amount)
     (let [protocol (create-protocol (vec (for [p (range amount)] (-->> 1 p (+ p 1)))))
           infra (generate-infrastructure protocol)
           channels (vec (for [p (range amount)] (get-channel p (+ p 1) infra)))
           msg (msg 1 1)
           time (custom-time
                  (doseq [_ (range iterations)]
                    (do (loop [pipe 0]
                          (do
                            (>!! (nth channels pipe) msg)
                            (<!! (nth channels pipe) 1)
                            (when (true? (< pipe (- amount 1)))
                              (recur (+ 1 pipe)))))
                        (force-monitor-reset! (get-monitor (first channels)))
                        )))]
       time))))
;(set-logging-exceptions)
;(discourje-pipeline 2)
;(discourje-pipeline 2)
;(discourje-pipeline 4)
;(discourje-pipeline 8)
;(discourje-pipeline 16)
(discourje-pipeline 16 16)
;(discourje-pipeline 64)
;(discourje-pipeline 128)
;(discourje-pipeline 256)

(defn clojure-pipeline
  ([amount]
   (let [channels (vec (for [_ (range amount)] (clojure.core.async/chan 1)))
         msg (msg 1 1)
         time (custom-time
                (loop [pipe 0]
                  (do
                    (clojure.core.async/>!! (nth channels pipe) msg)
                    (clojure.core.async/<!! (nth channels pipe))
                    (when (true? (< pipe (- amount 1)))
                      (recur (+ 1 pipe))))))]
     (doseq [c channels] (clojure.core.async/close! c))
     time))
  ([amount iterations]
   (if (<= iterations 1)
     (clojure-pipeline amount)
     (let [protocol (create-protocol (vec (for [p (range amount)] (-->> 1 p (+ p 1)))))
           infra (generate-infrastructure protocol)
           channels (vec (for [p (range amount)] (get-channel p (+ p 1) infra)))
           msg (msg 1 1)
           time (custom-time
                  (doseq [_ (range iterations)]
                    (do (loop [pipe 0]
                          (do
                            (clojure.core.async/>!! (get-chan (nth channels pipe)) msg)
                            (clojure.core.async/<!! (get-chan (nth channels pipe)))
                            (when (true? (< pipe (- amount 1)))
                              (recur (+ 1 pipe)))))
                        (force-monitor-reset! (get-monitor (first channels))))))]

       time))))

;(clojure-pipeline 2)
;(clojure-pipeline 4)
;(clojure-pipeline 8)
;(clojure-pipeline 16 16)
;(clojure-pipeline 32)
;(clojure-pipeline 64)
;(clojure-pipeline 128)
;(clojure-pipeline 256)