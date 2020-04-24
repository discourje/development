(ns discourje.core.async.examples.micro.mesh
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

(s/def ::spec-unbuffered
  [k]
  (s/ω (s/loop spec-outer [i 0]
               (s/if (< i k)
                 (s/alt (s/loop spec-inner [j 0]
                                (s/if (< j k)
                                  (s/alt (s/--> Boolean (::worker i) (::worker j))
                                         (s/recur spec-inner (inc j)))))
                        (s/recur spec-outer (inc i)))))))

(s/def ::spec-buffered
  [k]
  (s/loop spec-outer [i 0]
          (s/if (< i k)
            (s/par (s/loop spec-inner [j 0]
                           (s/if (< j k)
                             (s/par (s/ω (s/-->> Boolean (::worker i) (::worker j)))
                                    (s/recur spec-inner (inc j)))))
                   (s/recur spec-outer (inc i))))))

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
        (mapv (fn [i] (mapv (fn [j] (if (not= i j) (if buffered (a/chan 1) (a/chan)))) (range k))) (range k))

        ;; Link monitor [optional]
        _
        (if (= config/*lib* :dcj)
          (let [s (s/apply (if buffered ::spec-buffered ::spec-unbuffered) [k])
                m (dcj/monitor s)]
            (doseq [i (range k)
                    j (range k)]
              (if (not= i j)
                (dcj/link (nth (nth workers->workers i) j)
                          (s/role ::worker [i])
                          (s/role ::worker [j])
                          m)))))

        ;; Spawn threads
        workers
        (map (fn [i] (a/thread (let [ins (filterv (complement nil?) (mapv #(nth % i) workers->workers))
                                     outs (filterv (complement nil?) (nth workers->workers i))
                                     begin (System/nanoTime)
                                     deadline (+ begin (* secs 1000 1000 1000))]
                                 (loop [n-iter 0]
                                   (if (< (System/nanoTime) deadline)
                                     (if (first (a/alts!! (reduce into [(mapv #(vector % true) outs)
                                                                        ins
                                                                        [(a/timeout 100)]])))
                                       (recur (inc n-iter))
                                       (recur n-iter))
                                     [begin (System/nanoTime) n-iter])))))
             (range k))

        ;; Await termination
        output
        (let [[begin end n-iter] (reduce (fn [[begin1 end1 n-iter1]
                                              [begin2 end2 n-iter2]]
                                           [(min begin1 begin2)
                                            (max end1 end2)
                                            (+ n-iter1 n-iter2)])
                                         (map #(a/<!! %) workers))]
          [(- end begin) (quot n-iter 2)])

        ;; Stop timer
        end (System/nanoTime)]

    (set! config/*output* output)
    (set! config/*time* (- end begin))))
