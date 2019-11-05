(ns discourje.examples.tacas2020.npb3.FTThreads.spec
  (:require [discourje.core.async :refer :all]))

(import discourje.examples.tacas2020.npb3.DoneMessage)
(import discourje.examples.tacas2020.npb3.ExitMessage)
(import discourje.examples.tacas2020.npb3.FTThreads.EvolveMessage)
(import discourje.examples.tacas2020.npb3.FTThreads.FFTMessage)
(import discourje.examples.tacas2020.npb3.FTThreads.FFTSetVariablesMessage)

(def master (role "master"))
(def evolve (role "evolve"))
(def fft (role "fft"))

(def ft (dsl :k (fix :X (alt [(ins one-all-one master evolve :k
                                   discourje.examples.tacas2020.npb3.FTThreads.EvolveMessage
                                   discourje.examples.tacas2020.npb3.DoneMessage)
                              (fix :X)]
                             [(ins one-all-one master fft :k
                                   discourje.examples.tacas2020.npb3.FTThreads.FFTMessage
                                   discourje.examples.tacas2020.npb3.DoneMessage)
                              (fix :X)]
                             [(ins one-all-one master fft :k
                                   discourje.examples.tacas2020.npb3.FTThreads.FFTSetVariablesMessage
                                   discourje.examples.tacas2020.npb3.DoneMessage)
                              (fix :X)]
                             [(ins one-all-one master fft :k
                                   discourje.examples.tacas2020.npb3.ExitMessage
                                   discourje.examples.tacas2020.npb3.DoneMessage)
                              (ins one-all-one master evolve :k
                                  discourje.examples.tacas2020.npb3.ExitMessage
                                  discourje.examples.tacas2020.npb3.DoneMessage)
                              (rep seq [:i (range :k)]
                                   [(-## master (fft :i)) (-## (fft :i) master)
                                    (-## master (evolve :i)) (-## (evolve :i) master)])]))))

(defn s [k] (ins ft k))