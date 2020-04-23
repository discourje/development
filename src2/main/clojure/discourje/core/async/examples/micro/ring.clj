(ns discourje.core.async.examples.micro.ring
  (:require [clojure.core.async]
            [discourje.core.async :as dcj]
            [discourje.core.async.examples.config :as config]
            [discourje.spec :as s]))

(if (contains? (ns-aliases *ns*) 'a)
  (ns-unalias *ns* 'a))

(case config/*lib*
  :clj (alias 'a 'clojure.core.async)
  :dcj (alias 'a 'discourje.core.async)
  nil)

;;;;
;;;; Specification
;;;;

(s/defrole ::worker "worker")

(s/def ::ring [k] (s/loop omega []
                          [(s/loop ring [i 0]
                                   (s/if (< i k)
                                     [(s/-->> (::worker i) (::worker (mod (inc i) k)))
                                      (s/recur ring (inc i))]))
                           (s/recur omega)]))

;;;;
;;;; Implementation
;;;;

(let [input config/*input*
      k (:k input)
      secs (:secs input)]

  (let [m (dcj/monitor (s/apply ::ring [k]))

        ;; Create channels
        workers->workers
        (case config/*lib*
          :clj (mapv (fn [_] (a/chan 1)) (range k))
          :dcj (mapv (fn [i] (a/chan 1
                                     (s/role ::worker [i])
                                     (s/role ::worker [(mod (inc i) k)])
                                     m {})) (range k))
          nil)

        ;; Spawn threads
        workers
        (mapv #(cond

                 ;; Worker 0
                 (= % 0)
                 (a/thread (let [begin (System/nanoTime)
                                 deadline (+ begin (* secs 1000 1000 1000))
                                 in (nth workers->workers (dec k))
                                 out (nth workers->workers 0)]
                             (loop [n-iter 0]
                               (if (< (System/nanoTime) deadline)
                                 (do (a/>!! out true)
                                     (a/<!! in)
                                     (recur (inc n-iter)))
                                 (do (a/>!! out false)
                                     (a/<!! in)
                                     [(- (System/nanoTime) begin) n-iter])))))

                 ;; Worker i (0 < i < k)
                 (< 0 % k)
                 (a/thread (let [in (nth workers->workers (dec %))
                                 out (nth workers->workers %)]
                             (loop []
                               (if (a/<!! in)
                                 (do (a/>!! out true)
                                     (recur))
                                 (do (a/>!! out false)))))))

              (range k))

        ;; Await termination
        output
        (a/<!! (first workers))]

    (set! config/*output* output)))
