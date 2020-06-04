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

(s/defrole ::worker "worker")

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
      resolution (:resolution input)
      buffered (:buffered input)
      secs (:secs input)
      k (:k input)]

  (let [;; Start timer
        begin (System/nanoTime)

        ;; Create channels
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
        (a/thread (let [deadline (+ begin (* secs 1000 1000 1000))]
                    (loop [not-done true
                           timer (timer/timer resolution)]
                      (a/>!! (ring 0 1) not-done)
                      (a/<!! (ring (dec k) 0))
                      (if not-done
                        (recur (< (System/nanoTime) deadline) (timer/tick timer))
                        timer))))

        workers'
        (map (fn [i] (a/thread (loop []
                                 (let [v (a/<!! (ring (dec i) i))
                                       _ (a/>!! (ring i (mod (inc i) k)) v)]
                                   (if v (recur))))))
             (range 1 k))

        ;; Await termination
        output
        (do (doseq [worker workers']
              (a/<!! worker))
            (timer/report (a/<!! worker0)))

        ;; Stop timer
        end (System/nanoTime)]

    (set! config/*output* output)
    (set! config/*time* (- end begin))))
