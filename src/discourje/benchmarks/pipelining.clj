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
   (let [protocol (create-protocol (vec (for [p (range amount)] (-->> Long p (+ p 1)))))
         infra (generate-infrastructure protocol)
         channels (vec (for [p (range amount)] (get-channel infra p (+ p 1) )))
         msg 1
         time (custom-time
                (loop [pipe 0]
                  (do
                    (>!! (nth channels pipe) msg)
                    (<!!! (nth channels pipe))
                    (when (true? (< pipe (- amount 1)))
                      (recur (+ 1 pipe))))))]
     (close-infrastructure! infra)
     time))
  ([amount iterations]
   (if (<= iterations 1)
     (discourje-pipeline amount)
     (let [protocol (create-protocol (vec (for [p (range amount)] (-->> 1 p (+ p 1)))))
           infra (generate-infrastructure protocol)
           channels (vec (for [p (range amount)] (get-channel infra p (+ p 1) )))
           interactions (get-active-interaction (get-monitor (first channels)))
           msg 1
           time (custom-time
                  (doseq [_ (range iterations)]
                    (do (loop [pipe 0]
                          (do
                            (>!! (nth channels pipe) msg)
                            (<!! (nth channels pipe))
                            (when (true? (< pipe (- amount 1)))
                              (recur (+ 1 pipe)))))
                        (force-monitor-reset! (get-monitor (first channels)) interactions)
                        )))]
       time))))

(defn discourje-pipeline-new-reverse
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
  ([amount iterations]
   (let [protocol (create-protocol (vec (for [p (range (- amount 1))] (-->> Long p (+ p 1)))))
         infra (generate-infrastructure protocol)
         channels (vec (for [p (range (- amount 1))] (get-channel infra p (+ p 1) )))
         interactions (get-active-interaction (get-monitor (first channels)))
         msg 1
         max (- amount 2)
         time (cond
                (< amount 2) "invalid amount"
                (== amount 2) (custom-time
                                (doseq [_ (range iterations)]
                                  (do
                                    (do (thread (do (>!! (first channels) msg)))
                                        (<!! (first channels)))
                                    (force-monitor-reset! (get-monitor (first channels)) interactions))))
                :else
                (custom-time
                  (doseq [_ (range iterations)]
                    (do
                      (loop [element max]
                        (if (== 0 element)
                          (thread (>!! (first channels) msg))
                          (thread (do (<!! (nth channels (- element 1)))
                                      (>!! (nth channels element) msg))))
                        (when (< 0 element)
                          (recur (- element 1))))
                      (<!! (last channels))
                      (force-monitor-reset! (get-monitor (first channels)) interactions)))))]
     time)))

(defn clojure-pipeline-new-reverse
  ([amount iterations]
   (let [protocol (create-protocol (vec (for [p (range (- amount 1))] (-->> Long p (+ p 1)))))
         infra (generate-infrastructure protocol)
         channels (vec (for [p (range (- amount 1))] (get-channel infra p (+ p 1) )))
         interactions (get-active-interaction (get-monitor (first channels)))
         msg 1
         max (- amount 2)
         time (cond
                (< amount 2) "invalid amount!"
                (== amount 2) (custom-time
                                (doseq [_ (range iterations)]
                                  (do
                                    (do (thread (do (clojure.core.async/>!! (get-chan (first channels)) msg)))
                                        (clojure.core.async/<!! (get-chan (first channels))))
                                    (force-monitor-reset! (get-monitor (first channels)) interactions))))
                :else
                (custom-time
                  (doseq [_ (range iterations)]
                    (do
                      (loop [element max]
                        (if (== 0 element)
                          (thread (clojure.core.async/>!! (get-chan (first channels)) msg))
                          (thread (do (clojure.core.async/<!! (get-chan (nth channels (- element 1))))
                                      (clojure.core.async/>!! (get-chan (nth channels element)) msg))))
                        (when (< 0 element)
                          (recur (- element 1))))
                      (clojure.core.async/<!! (get-chan (last channels)))
                      (force-monitor-reset! (get-monitor (first channels)) interactions)))))]
     time)))

(defn discourje-pipeline-new
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
  ([amount iterations]
   (let [protocol (create-protocol (vec (for [p (range (- amount 1))] (-->> Long p (+ p 1)))))
         infra (generate-infrastructure protocol)
         channels (vec (for [p (range (- amount 1))] (get-channel infra p (+ p 1) )))
         interactions (get-active-interaction (get-monitor (first channels)))
         msg 1
         max (- amount 3)
         time (cond
                (< amount 2) "invalid amount!"
                (== amount 2) (custom-time
                                (doseq [_ (range iterations)]
                                  (do
                                    (do (thread (do (>!! (first channels) msg)))
                                        (<!! (first channels)))
                                    (force-monitor-reset! (get-monitor (first channels)) interactions))))
                :else
                (custom-time
                  (doseq [_ (range iterations)]
                    (do
                      (thread (>!! (first channels) msg))
                      (loop [element 0]
                        (thread (do (<!! (nth channels element))
                                    (>!! (nth channels (+ element 1)) msg)))
                        (when (< element max)
                          (recur (+ 1 element))))
                      (<!! (last channels))
                      (force-monitor-reset! (get-monitor (first channels)) interactions)))))]
     time)))

(defn clojure-pipeline-new
  ([amount iterations]
   (let [protocol (create-protocol (vec (for [p (range (- amount 1))] (-->> Long p (+ p 1)))))
         infra (generate-infrastructure protocol)
         channels (vec (for [p (range (- amount 1))] (get-channel infra p (+ p 1) )))
         interactions (get-active-interaction (get-monitor (first channels)))
         msg 1
         max (- amount 3)
         time (cond
                (< amount 2) "invalid amount!"
                (== amount 2) (custom-time
                                (doseq [_ (range iterations)]
                                  (do
                                    (do (thread (do (clojure.core.async/>!! (get-chan (first channels)) msg)))
                                        (clojure.core.async/<!! (get-chan (first channels))))
                                    (force-monitor-reset! (get-monitor (first channels)) interactions))))
                :else
                (custom-time
                  (doseq [_ (range iterations)]
                    (do
                      (thread (clojure.core.async/>!! (get-chan (first channels)) msg))
                      (loop [element 0]
                        (thread (do (clojure.core.async/<!! (get-chan (nth channels element)))
                                    (clojure.core.async/>!! (get-chan (nth channels (+ element 1))) msg)))
                        (when (< element max)
                          (recur (+ 1 element))))

                      (clojure.core.async/<!! (get-chan (last channels)))
                      (force-monitor-reset! (get-monitor (first channels)) interactions)))))]
     time)))

(defn clojure-pipeline
  ([amount]
   (let [channels (vec (for [_ (range amount)] (clojure.core.async/chan 1)))
         msg 1
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
           channels (vec (for [p (range amount)] (get-channel infra p (+ p 1) )))
           interactions (get-active-interaction (get-monitor (first channels)))
           msg 1
           time (custom-time
                  (doseq [_ (range iterations)]
                    (do (loop [pipe 0]
                          (do
                            (clojure.core.async/>!! (get-chan (nth channels pipe)) msg)
                            (clojure.core.async/<!! (get-chan (nth channels pipe)))
                            (when (true? (< pipe (- amount 1)))
                              (recur (+ 1 pipe)))))
                        (force-monitor-reset! (get-monitor (first channels)) interactions))))]

       time))))