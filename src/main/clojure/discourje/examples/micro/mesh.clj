(ns discourje.examples.micro.mesh
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

(s/defsession ::mesh-unbuffered [k]
  (s/* (s/alt-every [i (range k)
                     j (range k)]
         (s/--> Boolean (::worker i) (::worker j)))))

(s/defsession ::mesh-buffered [k]
  (s/par-every [i (range k)
                j (range k)]
    (s/* (s/-->> Boolean (::worker i) (::worker j)))))

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
        mesh
        (u/mesh (if buffered (fn [] (a/chan 1)) a/chan) (range k))

        ;; Link monitor [optional]
        _
        (if (= config/*lib* :dcj)
          (let [s (apply (if buffered mesh-buffered mesh-unbuffered) [k])
                m (a/monitor s)]
            (u/link-mesh mesh worker m)))

        ;; Spawn threads
        workers
        (map (fn [i] (a/thread (let [acts (reduce into [(u/puts mesh [i true] (remove #{i} (range k)))
                                                        (u/takes mesh (remove #{i} (range k)) i)
                                                        [(a/timeout 100)]])
                                     deadline (+ begin (* secs 1000 1000 1000))]
                                 (loop [not-done true
                                        timer (timer/timer resolution)]
                                   (let [[v _] (a/alts!! acts)]
                                     (if not-done
                                       (if v
                                         (recur (< (System/nanoTime) deadline) (timer/tick timer))
                                         (recur (< (System/nanoTime) deadline) timer))
                                       timer))))))
             (range k))

        ;; Await termination
        output
        (timer/report (apply timer/aggregate (map #(a/<!! %) workers)))

        ;; Stop timer
        end (System/nanoTime)]

    (set! config/*output* output)
    (set! config/*time* (- end begin))))
