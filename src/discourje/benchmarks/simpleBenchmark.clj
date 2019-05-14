(ns discourje.benchmarks.simpleBenchmark
  (require [discourje.core.async :refer :all]
           [discourje.core.logging :refer :all]
           [discourje.core.logging :refer :all]
           [criterium.core :refer :all]))

;(binding [*sample-count* 1 *warmup-jit-period* 0 *max-gc-attempts* 1](report-result (binding [*sample-count* 1 *warmup-jit-period* 0 *max-gc-attempts* 1](quick-benchmark (binding [*sample-count* 1 *warmup-jit-period* 0 *max-gc-attempts* 1](Thread/sleep 1)){:verbose true :sample-count 1}))))

(def coreasync-a-b (clojure.core.async/chan 1))

(defn core-async-send []
  (clojure.core.async/>!! coreasync-a-b "hi")
  (clojure.core.async/<!! coreasync-a-b ))
(binding [
          *default-quick-bench-opts*
          {:max-gc-attempts 100
           :samples 1024
           :target-execution-time 10
           :warmup-jit-period 10
           :tail-quantile 0.025
           :bootstrap-size 2}
          ]
  (with-progress-reporting (quick-bench (core-async-send) :verbose)))

;(defn testbinding[]
;  (binding [*report-progress* true
;            *sample-count* 0.1
;            *warmup-jit-period* 0
;            *max-gc-attempts* 1
;            ]
;    (quick-bench (Thread/sleep 1))) )
;(testbinding)
;
;;(* 10 s-to-ns) (* 1 s-to-ns)
;(run-benchmark 1 (* 10 s-to-ns) 1 (fn[] (Thread/sleep 1)) 1 1)


;(quick-bench (Thread/sleep 10))
(def protocol
  (mep
    (-->> "hi" "Alice" "Bob")))
(def infra (generate-infrastructure protocol))
(def discourje-a-b (get-channel "Alice" "Bob" infra))
(def m (msg "hi" "foo"))

(defn discourje-send []
  (>!! discourje-a-b m)
  (<!! discourje-a-b "hi"))

(time (dotimes [_ 1000] (core-async-send)))
;(time (clojure.core.async/thread (core-async-send)))
;(time (thread (discourje-send)))
;(run-benchmark 1 (* 10 s-to-ns) (* 1 s-to-ns) (fn[] (clojure.core.async/thread (core-async-send))) 1 1)