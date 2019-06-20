(ns discourje.benchmarks.benchmarkRunner
  (:require [discourje.benchmarks.OneBuyer :refer :all]
            [discourje.benchmarks.TwoBuyer :refer :all]
            [discourje.benchmarks.pipelining :refer :all]
            [discourje.benchmarks.scatterGather :refer :all]
            [discourje.core.logging :refer :all]))
;main.clj
;(in-ns 'discourje.core.async)

(defn start-discourje-one-buyer [iterations]
  (discourje.benchmarks.OneBuyer/discourje-one-buyer-monitor-reset iterations))

(defn start-clojure-one-buyer [iterations]
  (discourje.benchmarks.OneBuyer/clojure-one-buyer-reset iterations))

(defn start-discourje-two-buyers [iterations]
  (discourje.benchmarks.TwoBuyer/discourje-two-buyer-monitor-reset iterations))

(defn start-clojure-two-buyers [iterations]
  (discourje.benchmarks.TwoBuyer/clojure-two-buyer-reset iterations))

(defn start-discourje-pipeline [amount iterations]
  (discourje.benchmarks.pipelining/discourje-pipeline amount iterations))

(defn start-clojure-pipeline [amount iterations]
  (discourje.benchmarks.pipelining/clojure-pipeline amount iterations))

(defn start-discourje-scattergather [workers iterations]
  (discourje.benchmarks.scatterGather/discourje-scatter-gather workers iterations))

(defn start-clojure-scattergather [workers iterations]
  (discourje.benchmarks.scatterGather/clojure-scatter-gather workers iterations))

(defn set-exceptions-only [] (set-logging-exceptions))
(defn set-logging-only [] (set-logging))
(defn set-both-logging-and-exceptions [] (set-logging-and-exceptions))
