;(ns discourje.examples.clbg.mandelbrot
;  (:require [discourje.core.async :refer :all]))
;
;(def master (role "master"))
;(def worker (role "worker"))
;(def all-workers (role "all-workers"))
;
;;(def mandelbrot (dsl :k (fix :X (par [(--> all-workers master Long)
;;                                      (rep alt (:i (range :k))
;;                                           (--> master (worker :i)))]
;;                                     (fix :X)))))
;
;;(def mandelbrot (dsl :k (rep par (:i (range :k))
;;                             (fix [:X :i]
;;                                  [(--> all-workers master Integer)
;;                                   (--> master (worker :i) Integer)
;;                                   (fix [:X :i])]))))
;
;(def mandelbrot (dsl :k (par
;                          (fix :X [(--> all-workers master Integer)
;                                   (fix :X)])
;                          (rep par (:i (range :k))
;                               (fix [:X :i]
;                                    [(--> master (worker :i) Integer)
;                                     (fix [:X :i])])))))
;
;(defn s [k] (ins mandelbrot k))
;
;(s 2)