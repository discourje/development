(ns discourje.core.async.examples.micro.star
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

(s/defrole ::master "master")
(s/defrole ::worker "worker")

(s/def ::spec-cat
  [k]
  (s/ω (s/loop spec [i 0]
               (s/if (< i k)
                 (s/cat (s/cat (s/--> Boolean ::master (::worker i))
                               (s/--> Boolean (::worker i) ::master))
                        (s/recur spec (inc i)))))))

(s/def ::spec-alt
  [k]
  (s/ω (s/loop spec [i 0]
               (s/if (< i k)
                 (s/alt (s/cat (s/--> Boolean ::master (::worker i))
                               (s/--> Boolean (::worker i) ::master))
                        (s/recur spec (inc i)))))))

(s/def ::spec-par
  [k]
  (s/ω (s/loop spec [i 0]
               (s/if (< i k)
                 (s/par (s/cat (s/-->> Boolean ::master (::worker i))
                               (s/-->> Boolean (::worker i) ::master))
                        (s/recur spec (inc i)))))))

;;;;
;;;; Implementation
;;;;

(let [input config/*input*
      buffered (:buffered input)
      ordered-sends (:ordered-sends input)
      ordered-receives (:ordered-receives input)
      k (:k input)
      secs (:secs input)]

  (let [;; Start timer
        begin (System/nanoTime)

        ;; Create channels
        master->workers
        (mapv (fn [_] (if buffered (a/chan 1) (a/chan))) (range k))
        workers->master
        (mapv (fn [_] (if buffered (a/chan 1) (a/chan))) (range k))

        ;; Link monitor [optional]
        _
        (if (= config/*lib* :dcj)
          (let [s (s/apply (cond (and buffered)
                                 ::spec-par
                                 (and (not buffered) ordered-sends)
                                 ::spec-cat
                                 (and (not buffered) (not ordered-sends))
                                 ::spec-alt)
                           [k])
                m (dcj/monitor s)]
            (doseq [i (range k)] (dcj/link (nth master->workers i)
                                           (s/role ::master)
                                           (s/role ::worker [i])
                                           m))
            (doseq [i (range k)] (dcj/link (nth workers->master i)
                                           (s/role ::worker [i])
                                           (s/role ::master)
                                           m))))

        ;; Spawn threads
        master
        (a/thread (let [begin (System/nanoTime)
                        deadline (+ begin (* secs 1000 1000 1000))]
                    (cond

                      ;; Buffered, ordered sends, ordered receives
                      (and buffered ordered-sends ordered-receives)
                      (loop [n-iter 0
                             not-done true]
                        (doseq [out master->workers]
                          (a/>!! out not-done))
                        (doseq [in workers->master]
                          (a/<!! in))
                        (if not-done
                          (recur (inc n-iter) (< (System/nanoTime) deadline))
                          [(- (System/nanoTime) begin) n-iter]))

                      ;; Buffered, ordered sends, unordered receives
                      (and buffered ordered-sends (not ordered-receives))
                      (loop [n-iter 0
                             not-done true]
                        (doseq [out master->workers]
                          (a/>!! out not-done))
                        (loop [ins workers->master]
                          (if (not (empty? ins))
                            (let [[_ in] (a/alts!! ins)]
                              (recur (remove #(= in %) ins)))))
                        (if not-done
                          (recur (inc n-iter) (< (System/nanoTime) deadline))
                          [(- (System/nanoTime) begin) n-iter]))

                      ;; Buffered, unordered sends, ordered receives
                      (and buffered (not ordered-sends) ordered-receives)
                      (loop [n-iter 0
                             not-done true]
                        (loop [outs master->workers]
                          (if (not (empty? outs))
                            (let [[_ out] (a/alts!! (mapv #(vector % not-done) outs))]
                              (recur (remove #(= out %) outs)))))
                        (doseq [in workers->master]
                          (a/<!! in))
                        (if not-done
                          (recur (inc n-iter) (< (System/nanoTime) deadline))
                          [(- (System/nanoTime) begin) n-iter]))

                      ;; Buffered, unordered sends, unordered receives
                      (and buffered (not ordered-sends) (not ordered-receives))
                      (loop [n-iter 0
                             not-done true]
                        (loop [outs master->workers]
                          (if (not (empty? outs))
                            (let [[_ out] (a/alts!! (mapv #(vector % not-done) outs))]
                              (recur (remove #(= out %) outs)))))
                        (loop [ins workers->master]
                          (if (not (empty? ins))
                            (let [[_ in] (a/alts!! ins)]
                              (recur (remove #(= in %) ins)))))
                        (if not-done
                          (recur (inc n-iter) (< (System/nanoTime) deadline))
                          [(- (System/nanoTime) begin) n-iter]))

                      ;; Unbuffered, ordered sends
                      (and (not buffered) ordered-sends)
                      (loop [n-iter 0
                             not-done true]
                        (doseq [i (range k)]
                          (a/>!! (nth master->workers i) not-done)
                          (a/<!! (nth workers->master i)))
                        (if not-done
                          (recur (inc n-iter) (< (System/nanoTime) deadline))
                          [(- (System/nanoTime) begin) n-iter]))

                      ;; Unbuffered, unordered sends
                      (and (not buffered) (not ordered-sends))
                      (loop [n-iter 0
                             not-done true]
                        (loop [outs-ins (zipmap master->workers workers->master)]
                          (if (not (empty? outs-ins))
                            (let [[_ out] (a/alts!! (mapv #(vector % not-done) (keys outs-ins)))
                                  in (get outs-ins out)]
                              (a/<!! in)
                              (recur (dissoc outs-ins out)))))
                        (if not-done
                          (recur (inc n-iter) (< (System/nanoTime) deadline))
                          [(- (System/nanoTime) begin) n-iter])))))

        workers
        (mapv #(a/thread (let [in (nth master->workers %)
                               out (nth workers->master %)]
                           (loop []
                             (let [v (a/<!! in)
                                   _ (a/>!! out v)]
                               (if v (recur))))))
              (range k))

        ;; Await termination
        output
        (do (doseq [worker workers]
              (a/<!! worker))
            (a/<!! master))

        ;; Stop timer
        end (System/nanoTime)]

    (set! config/*output* output)
    (set! config/*time* (- end begin))))
