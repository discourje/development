(ns discourje.examples.experimental.util
  (require [discourje.core.async :refer :all]))

;;
;; Settings
;;

(discourje.core.async/enable-wildcard)
(discourje.core.logging/set-logging-exceptions)
(discourje.core.logging/set-throwing true)
(reset! discourje.core.async/<!!-unwrap true)

;;
;; Patterns
;;

(def succ-fg
  (dsl :worker :i :type :f :g
       (vec (remove nil? [(when (not= :f nil) :f)
                          (--> (:worker :i) (:worker (+ :i 1)) :type)
                          (when (not= :g nil) :g)]))))

(def succ
  (dsl :worker :i :type
       [(--> (:worker :i) (:worker (+ :i 1)) :type)]))

(def pipe
  (dsl :worker :k :type
       (rep seq [:i (range (- :k 1))] (insert succ :worker :i :type))))

(def ring
  (dsl :worker :k :type
       [(insert pipe :worker :k :type)
        (--> (:worker (- :k 1)) (:worker 0) :type)]))

;;
;; Macros
;;

(defmacro forv [seq-exprs body-expr]
  `(vec (for ~seq-exprs ~body-expr)))

;;
;; Functions
;;

(defn join
  [threads]
  (if (vector? threads)
    (doseq [t threads] (clojure.core.async/<!! t))
    (clojure.core.async/<!! threads)))

(defn bench
  [time f]
  (let [begin (System/nanoTime)
        deadline (+ begin (* time 1000 1000 1000))]
    (loop [n 0]
      (f)
      (let [end (System/nanoTime)
            n' (+ n 1)]
        (if (< end deadline)
          (recur n')
          (println (- end begin) "ns,"
                   n' "runs,"
                   (quot (- end begin) n') "ns/run"))))))