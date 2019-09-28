(ns discourje.examples.experimental.api
  (:gen-class))

;;
;; Settings
;;

(discourje.core.async/enable-wildcard)

(discourje.core.logging/set-logging-exceptions)
(discourje.core.logging/set-throwing true)

;;
;; Api
;;

(defmacro thread
  [body]
  `(discourje.core.async/thread ~body))

(defn join
  [threads]
  (if (vector? threads)
    (doseq [t threads] (clojure.core.async/<!! t))
    (clojure.core.async/<!! threads)))

(defn chan
  [n sender receiver monitor]
  (discourje.core.async/->channel sender
                                  receiver
                                  (clojure.core.async/chan n)
                                  n
                                  monitor))

(defn >!!
  [chan content]
  (discourje.core.async/>!! chan content))

(defn <!!
  [chan]
  (let [message (discourje.core.async/<!! chan)
        content (discourje.core.async/get-content message)]
    content))

;;
;; Misc
;;

(defmacro forv [seq-exprs body-expr]
  `(vec (for ~seq-exprs ~body-expr)))

;;
;; Benchmarking
;;

(defn bench [time f]
  (let [begin (System/nanoTime)
        deadline (+ begin (* time 1000 1000 1000))]
    (loop [n 0]
      (f)
      (let [end (System/nanoTime)
            n' (+ n 1)]
        (if (< end deadline)
          (recur n')
          (println (- end begin) "ns,"
                   n' "run,"
                   (quot (- end begin) n') "ns/run"))))))