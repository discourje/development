;(ns discourje.examples.clbg.spectral-norm
;  (:require [discourje.core.async :refer :all]))
;
;(def worker (role "worker"))
;
;(def spectral-norm (dsl :k (fix :X [(ins ring worker :k Integer)
;                                    (fix :X)])))
;
;(defn s [k] (ins spectral-norm k))