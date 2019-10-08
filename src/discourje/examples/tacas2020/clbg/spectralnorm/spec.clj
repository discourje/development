(ns discourje.examples.tacas2020.clbg.spectralnorm.spec
  (require [discourje.core.async :refer :all]))

(def worker (role "worker"))

(def spectral-norm (dsl :k (fix :X [(insert ring worker :k Integer)
                                    (fix :X)])))

(defn s [k] (insert spectral-norm k))