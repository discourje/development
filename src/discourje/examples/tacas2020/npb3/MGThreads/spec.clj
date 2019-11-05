(ns discourje.examples.tacas2020.npb3.MGThreads.spec
  (require [discourje.core.async :refer :all]))

(import discourje.examples.tacas2020.npb3.DoneMessage)
(import discourje.examples.tacas2020.npb3.ExitMessage)
(import discourje.examples.tacas2020.npb3.MGThreads.InterpMessage)
(import discourje.examples.tacas2020.npb3.MGThreads.PsinvMessage)
(import discourje.examples.tacas2020.npb3.MGThreads.ResidMessage)
(import discourje.examples.tacas2020.npb3.MGThreads.RprjMessage)

(def master (role "master"))
(def interp (role "interp"))
(def psinv (role "psinv"))
(def resid (role "resid"))
(def rprj (role "rprj"))

(def ft (dsl :k (fix :X (alt [(ins one-all-one master interp :k
                                   discourje.examples.tacas2020.npb3.MGThreads.InterpMessage
                                   discourje.examples.tacas2020.npb3.DoneMessage)
                              (fix :X)]
                             [(ins one-all-one master psinv :k
                                   discourje.examples.tacas2020.npb3.MGThreads.PsinvMessage
                                   discourje.examples.tacas2020.npb3.DoneMessage)
                              (fix :X)]
                             [(ins one-all-one master resid :k
                                   discourje.examples.tacas2020.npb3.MGThreads.ResidMessage
                                   discourje.examples.tacas2020.npb3.DoneMessage)
                              (fix :X)]
                             [(ins one-all-one master rprj :k
                                   discourje.examples.tacas2020.npb3.MGThreads.RprjMessage
                                   discourje.examples.tacas2020.npb3.DoneMessage)
                              (fix :X)]
                             [(ins one-all-one master interp :k
                                   discourje.examples.tacas2020.npb3.ExitMessage
                                   discourje.examples.tacas2020.npb3.DoneMessage)
                              (ins one-all-one master psinv :k
                                   discourje.examples.tacas2020.npb3.ExitMessage
                                   discourje.examples.tacas2020.npb3.DoneMessage)
                              (ins one-all-one master rprj :k
                                   discourje.examples.tacas2020.npb3.ExitMessage
                                   discourje.examples.tacas2020.npb3.DoneMessage)
                              (ins one-all-one master resid :k
                                   discourje.examples.tacas2020.npb3.ExitMessage
                                   discourje.examples.tacas2020.npb3.DoneMessage)
                              (rep seq [:i (range :k)]
                                   [(-## master (interp :i)) (-## (interp :i) master)
                                    (-## master (psinv :i)) (-## (psinv :i) master)
                                    (-## master (rprj :i)) (-## (rprj :i) master)
                                    (-## master (resid :i)) (-## (resid :i) master)])]))))

(defn s [k] (ins ft k))