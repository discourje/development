(ns discourje.examples.micro.ring
  (:require [clojure.core.async]
            [discourje.core.async]
            [discourje.core.util :as u]
            [discourje.core.spec :as s]
            [discourje.core.lint :as l]
            [discourje.examples.config :as config]))

;;;;
;;;; Specification
;;;;

(s/defrole ::worker)

(s/defsession ::ring-unbuffered [k]
  (s/* (s/cat-every [i (range k)]
         (s/--> Boolean (::worker i) (::worker (mod (inc i) k))))))

(s/defsession ::ring-buffered [k]
  (s/* (s/cat-every [i (range k)]
         (s/-->> Boolean (::worker i) (::worker (mod (inc i) k))))))

;;;;
;;;; Implementation
;;;;

(defn spec []
  (condp = (:flags config/*input*)
    #{:unbuffered}
    (ring-unbuffered (:k config/*input*))
    #{:buffered}
    (ring-buffered (:k config/*input*))))

(config/clj-or-dcj)

(when (some? config/*lint*)
  (set! config/*output* (l/lint (spec))))

(when (some? config/*run*)
  (let [input config/*input*
        flags (:flags input)
        k (:k input)
        n (:n input)]

    (let [;; Create channels
          ring
          (cond (contains? flags :unbuffered)
                (u/ring a/chan (range k))
                (contains? flags :buffered)
                (u/ring (partial a/chan 1) (range k)))

          ;; Link monitor [optional]
          _
          (if (= config/*run* :dcj)
            (let [m (a/monitor (spec))]
              (u/link-ring ring worker m)))

          ;; Spawn threads
          worker0
          (a/thread (doseq [_ (range n)]
                      (a/>!! (ring 0 1) true)
                      (a/<!! (ring (dec k) 0))))

          workers'
          (mapv (fn [i] (a/thread (doseq [_ (range n)]
                                    (a/<!! (ring (dec i) i))
                                    (a/>!! (ring i (mod (inc i) k)) true))))
                (range 1 k))

          ;; Await termination
          output
          (do (a/<!! worker0)
              (doseq [worker workers']
                (a/<!! worker)))]

      (set! config/*output* output))))