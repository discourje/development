(ns discourje.examples.tacas2020.npb3.CGThreads.spec
  (require [discourje.core.async :refer :all]))

(import discourje.examples.tacas2020.npb3.DoneMessage)
(import discourje.examples.tacas2020.npb3.ExitMessage)
(import discourje.examples.tacas2020.npb3.CGThreads.CGMessage)

(def master (role "master"))
(def worker (role "worker"))

(def cg (dsl :k (fix :X (alt [(ins one-all-one master worker :k
                                   discourje.examples.tacas2020.npb3.CGThreads.CGMessage
                                   discourje.examples.tacas2020.npb3.DoneMessage)
                              (fix :X)]
                             (ins one-all-one master worker :k
                                  discourje.examples.tacas2020.npb3.ExitMessage
                                  discourje.examples.tacas2020.npb3.DoneMessage)))))

(defn s [k] (ins cg k))