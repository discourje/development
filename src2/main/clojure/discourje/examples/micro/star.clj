(ns discourje.examples.micro.star
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

(s/defrole ::master "master")
(s/defrole ::worker "worker")

(s/defsession ::star-cat [k]
  (s/* (s/cat-every [i (range k)]
         (s/cat (s/--> Boolean ::master (::worker i))
                (s/--> Boolean (::worker i) ::master)))))

(s/defsession ::star-alt [k]
  (s/* (s/alt-every [i (range k)]
         (s/cat (s/--> Boolean ::master (::worker i))
                (s/--> Boolean (::worker i) ::master)))))

(s/defsession ::star-par [k]
  (s/* (s/par-every [i (range k)]
         (s/cat (s/-->> Boolean ::master (::worker i))
                (s/-->> Boolean (::worker i) ::master)))))

;;;;
;;;; Implementation
;;;;

(let [input config/*input*
      resolution (:resolution input)
      buffered (:buffered input)
      ordered-sends (:ordered-sends input)
      ordered-receives (:ordered-receives input)
      secs (:secs input)
      k (:k input)]

  (let [;; Start timer
        begin (System/nanoTime)

        ;; Create channels
        star
        (u/star (if buffered (fn [] (a/chan 1)) a/chan) nil (range k))

        ;; Link monitor [optional]
        _
        (if (= config/*lib* :dcj)
          (let [s (apply (cond (and buffered)
                               star-par
                               (and (not buffered) ordered-sends)
                               star-cat
                               (and (not buffered) (not ordered-sends))
                               star-alt)
                         [k])
                m (a/monitor s)]

            (u/link-star star master worker m)))

        ;; Spawn threads
        master
        (a/thread (let [deadline (+ begin (* secs 1000 1000 1000))]
                    (cond

                      ;; Buffered, ordered sends, ordered receives
                      (and buffered ordered-sends ordered-receives)
                      (loop [not-done true
                             timer (timer/timer resolution)]
                        (doseq [[out _] (u/puts star [nil nil] (range k))]
                          (a/>!! out not-done))
                        (doseq [in (u/takes star (range k) nil)]
                          (a/<!! in))
                        (if not-done
                          (recur (< (System/nanoTime) deadline) (timer/tick timer))
                          timer))

                      ;; Buffered, ordered sends, unordered receives
                      (and buffered ordered-sends (not ordered-receives))
                      (loop [not-done true
                             timer (timer/timer resolution)]
                        (doseq [[out _] (u/puts star [nil nil] (range k))]
                          (a/>!! out not-done))
                        (loop [acts (u/takes star (range k) nil)]
                          (if (not (empty? acts))
                            (let [[_ c] (a/alts!! acts)]
                              (recur (remove #{c} acts)))))
                        (if not-done
                          (recur (< (System/nanoTime) deadline) (timer/tick timer))
                          timer))

                      ;; Buffered, unordered sends, ordered receives
                      (and buffered (not ordered-sends) ordered-receives)
                      (loop [not-done true
                             timer (timer/timer resolution)]
                        (loop [acts (u/puts star [nil not-done] (range k))]
                          (if (not (empty? acts))
                            (let [[_ c] (a/alts!! acts)]
                              (recur (remove #{[c not-done]} acts)))))
                        (doseq [in (u/takes star (range k) nil)]
                          (a/<!! in))
                        (if not-done
                          (recur (< (System/nanoTime) deadline) (timer/tick timer))
                          timer))

                      ;; Buffered, unordered sends, unordered receives
                      (and buffered (not ordered-sends) (not ordered-receives))
                      (loop [not-done true
                             timer (timer/timer resolution)]
                        (loop [acts (u/puts star [nil not-done] (range k))]
                          (if (not (empty? acts))
                            (let [[_ c] (a/alts!! acts)]
                              (recur (remove #{[c not-done]} acts)))))
                        (loop [acts (u/takes star (range k) nil)]
                          (if (not (empty? acts))
                            (let [[_ c] (a/alts!! acts)]
                              (recur (remove #{c} acts)))))
                        (if not-done
                          (recur (< (System/nanoTime) deadline) (timer/tick timer))
                          timer))

                      ;; Unbuffered, ordered sends
                      (and (not buffered) ordered-sends)
                      (loop [not-done true
                             timer (timer/timer resolution)]
                        (doseq [i (range k)]
                          (a/>!! (star nil i) not-done)
                          (a/<!! (star i nil)))
                        (if not-done
                          (recur (< (System/nanoTime) deadline) (timer/tick timer))
                          timer))

                      ;; Unbuffered, unordered sends
                      (and (not buffered) (not ordered-sends))
                      (loop [not-done true
                             timer (timer/timer resolution)]
                        (loop [acts (u/puts star [nil not-done] (range k))]
                          (if (not (empty? acts))
                            (let [[_ c] (a/alts!! acts)]
                              (a/<!! (star (u/taker-id star c) nil))
                              (recur (remove #{[c not-done]} acts)))))
                        (if not-done
                          (recur (< (System/nanoTime) deadline) (timer/tick timer))
                          timer)))))

        workers
        (mapv (fn [i] (a/thread (loop []
                                  (let [v (a/<!! (star nil i))
                                        _ (a/>!! (star i nil) v)]
                                    (if v (recur))))))
              (range k))

        ;; Await termination
        output
        (do (doseq [worker workers]
              (a/<!! worker))
            (timer/report (a/<!! master)))

        ;; Stop timer
        end (System/nanoTime)]

    (set! config/*output* output)
    (set! config/*time* (- end begin))))
