(ns discourje.examples.tacas2020.clbg.spectralnorm.spec
  (require [discourje.core.async :refer :all]))

(def worker (role "worker"))

(def spectral-norm (dsl :k (fix :X [(ins ring worker :k Integer)
                                    (fix :X)])))

(defn s [k] (ins spectral-norm k))