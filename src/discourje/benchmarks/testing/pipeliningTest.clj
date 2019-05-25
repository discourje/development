(ns discourje.benchmarks.testing.pipeliningTest
  (require [discourje.core.async :refer :all]
           [discourje.core.logging :refer :all]
           [criterium.core :refer :all]))


(def coreasync-a-b (clojure.core.async/chan 1))

(defn core-async-send []
  (clojure.core.async/>!! coreasync-a-b "hi")
  (clojure.core.async/<!! coreasync-a-b)
  (clojure.core.async/>!! coreasync-a-b "hi")
  (clojure.core.async/<!! coreasync-a-b)
  (clojure.core.async/>!! coreasync-a-b "hi")
  (clojure.core.async/<!! coreasync-a-b)
  (clojure.core.async/>!! coreasync-a-b "hi")
  (clojure.core.async/<!! coreasync-a-b)
  (clojure.core.async/>!! coreasync-a-b "hi")
  (clojure.core.async/<!! coreasync-a-b)
  (clojure.core.async/>!! coreasync-a-b "hi")
  (clojure.core.async/<!! coreasync-a-b)
  (clojure.core.async/>!! coreasync-a-b "hi")
  (clojure.core.async/<!! coreasync-a-b)
  (clojure.core.async/>!! coreasync-a-b "hi")
  (clojure.core.async/<!! coreasync-a-b)
  (clojure.core.async/>!! coreasync-a-b "hi")
  (clojure.core.async/<!! coreasync-a-b)
  (clojure.core.async/>!! coreasync-a-b "hi")
  (clojure.core.async/<!! coreasync-a-b))
(binding [
          *default-quick-bench-opts*
          {:max-gc-attempts       100
           :samples               1025
           :target-execution-time 10
           :warmup-jit-period     10
           :tail-quantile         0.025
           :bootstrap-size        2}
          ]
  (with-progress-reporting (quick-bench (core-async-send) :verbose)))

(time (dotimes [_ 1000] (core-async-send)))
(def protocol
  (mep
    (-->> "hi" "Alice" "Bob")
    (-->> "hi" "Bob" "Alice")
    (-->> "hi" "Alice" "Bob")
    (-->> "hi" "Bob" "Alice")
    (-->> "hi" "Alice" "Bob")
    (-->> "hi" "Bob" "Alice")
    (-->> "hi" "Alice" "Bob")
    (-->> "hi" "Bob" "Alice")
    (-->> "hi" "Alice" "Bob")
    (-->> "hi" "Bob" "Alice")))

(def infra (generate-infrastructure protocol))
(def discourje-a-b (get-channel "Alice" "Bob" infra))
(def m (msg "hi" "foo"))
(set-logging-exceptions)
(defn discourje-send []
  (>!! discourje-a-b m)
  (<!! discourje-a-b "hi")
  (force-monitor-reset! (get-monitor discourje-a-b)))

(binding [
          *default-quick-bench-opts*
          {:max-gc-attempts       100
           :samples               1025
           :target-execution-time 10
           :warmup-jit-period     10
           :tail-quantile         0.025
           :bootstrap-size        2}
          ]
  (with-progress-reporting (quick-bench (discourje-send) :verbose)))
(time (dotimes [_ 1000] (discourje-send)))

(def rec-protocol
  (mep
    (rec :rec
         (-->> "hi" "Alice" "Bob")
         (continue :rec))))

(def rec-infra (generate-infrastructure rec-protocol))
(def discourje-a-b (get-channel "Alice" "Bob" rec-infra))
(defn discourje-rec-send []
  (>!! discourje-a-b m)
  (<!! discourje-a-b "hi"))
(binding [
          *default-quick-bench-opts*
          {:max-gc-attempts       100
           :samples               1025
           :target-execution-time 10
           :warmup-jit-period     10
           :tail-quantile         0.025
           :bootstrap-size        2}
          ]
  (with-progress-reporting (quick-bench (discourje-rec-send) :verbose)))
(time (dotimes [_ 1000] (discourje-rec-send)))