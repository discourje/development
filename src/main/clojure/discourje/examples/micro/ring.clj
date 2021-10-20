(ns discourje.examples.micro.ring
  (:require [clojure.core.async]
            [discourje.core.async]
            [discourje.core.util :as u]
            [discourje.core.spec :as s]
            [discourje.examples.config :as config]
            [discourje.examples.timer :as timer]))

(config/clj-or-dcj)

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

(let [input config/*input*
      buffered (:buffered input)
      k (:k input)
      n (:n input)]

  (let [;; Create channels
        ring
        (u/ring (if buffered (fn [] (a/chan 1)) a/chan) (range k))

        ;; Link monitor [optional]
        _
        (if (= config/*lib* :dcj)
          (let [s (apply (if buffered ring-buffered ring-unbuffered) [k])
                m (a/monitor s)]
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

    (set! config/*output* output)))
