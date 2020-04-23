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
  :dcj-nil (alias 'a 'discourje.core.async)
  nil)

;;;;
;;;; Specification
;;;;

(s/defrole ::worker "worker")

(s/def ::ring-unbuffered
  [k]
  (s/* (s/loop ring [i 0]
               (s/if (< i k)
                 (s/cat (s/--> (::worker i) (::worker (mod (inc i) k)))
                        (s/recur ring (inc i)))))))

(s/def ::ring-buffered
  [k]
  (s/* (s/loop ring [i 0]
               (s/if (< i k)
                 (s/cat (s/-->> (::worker i) (::worker (mod (inc i) k)))
                        (s/recur ring (inc i)))))))

;;;;
;;;; Implementation
;;;;

(let [input config/*input*
      buffered (:buffered input)
      k (:k input)
      secs (:secs input)]

  (let [;; Start timer
        begin (System/nanoTime)

        ;; Create channels
        workers->workers
        (mapv (fn [_] (if buffered (a/chan 1) (a/chan))) (range k))

        ;; Link monitor [optional]
        _
        (if (= config/*lib* :dcj)
          (let [s (s/apply (if buffered ::ring-buffered ::ring-unbuffered) [k])
                m (dcj/monitor s)]
            (doseq [i (range k)] (dcj/link (nth workers->workers i)
                                           (s/role ::worker [i])
                                           (s/role ::worker [(mod (inc i) k)])
                                           m))))

        ;; Spawn threads
        workers
        (mapv #(cond

                 ;; Worker 0
                 (= % 0)
                 (a/thread (let [in (nth workers->workers (dec k))
                                 out (nth workers->workers 0)
                                 begin (System/nanoTime)
                                 deadline (+ begin (* secs 1000 1000 1000))]
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
        (a/<!! (first workers))

        ;; Stop timer
        end (System/nanoTime)]

    (set! config/*output* output)
    (set! config/*time* (- end begin))))
