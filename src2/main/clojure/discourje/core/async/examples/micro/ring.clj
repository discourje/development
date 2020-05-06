(ns discourje.core.async.examples.micro.ring
  (:require [clojure.core.async]
            [discourje.core.async :as dcj]
            [discourje.core.async.examples.config :as config]
            [discourje.core.async.examples.timer :as timer]
            [discourje.spec :as s]))

(if (contains? (ns-aliases *ns*) 'a)
  (ns-unalias *ns* 'a))

(case config/*lib*
  :clj (alias 'a 'clojure.core.async)
  :dcj (alias 'a 'discourje.core.async)
  :dcj-nil (alias 'a 'discourje.core.async)
  nil)

;;;;
;;;; Specification
;;;;

(s/defrole ::worker "worker")

(s/defsession ::spec-unbuffered
              [k]
              (s/ω (s/loop spec [i 0]
               (s/if (< i k)
                 (s/cat (s/--> Boolean (::worker i) (::worker (mod (inc i) k)))
                        (s/recur spec (inc i)))))))

(s/defsession ::spec-buffered
              [k]
              (s/ω (s/loop spec [i 0]
               (s/if (< i k)
                 (s/cat (s/-->> Boolean (::worker i) (::worker (mod (inc i) k)))
                        (s/recur spec (inc i)))))))

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
        workers->workers
        (mapv (fn [_] (if buffered (a/chan 1) (a/chan))) (range k))

        ;; Link monitor [optional]
        _
        (if (= config/*lib* :dcj)
          (let [s (s/apply (if buffered ::spec-buffered ::spec-unbuffered) [k])
                m (dcj/monitor s)]
            (doseq [i (range k)] (dcj/link (nth workers->workers i)
                                           (s/role ::worker [i])
                                           (s/role ::worker [(mod (inc i) k)])
                                           m))))

        ;; Spawn threads
        worker0
        (a/thread (let [in (nth workers->workers (dec k))
                        out (nth workers->workers 0)
                        deadline (+ begin (* secs 1000 1000 1000))]
                    (loop [not-done true
                           timer (timer/timer resolution)]
                      (a/>!! out not-done)
                      (a/<!! in)
                      (if not-done
                        (recur (< (System/nanoTime) deadline) (timer/tick timer))
                        timer))))

        workers'
        (map #(a/thread (let [in (nth workers->workers (dec %))
                              out (nth workers->workers %)]
                          (loop []
                            (let [v (a/<!! in)
                                  _ (a/>!! out v)]
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
