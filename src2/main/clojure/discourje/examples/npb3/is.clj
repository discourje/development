(ns discourje.examples.npb3.is
  (:require [clojure.core.async]
            [discourje.core.async]
            [discourje.core.spec :as s]
            [discourje.examples.config :as config]
            [discourje.examples.timer :as timer])
  (:import (discourje.examples.npb3 Config)
           (discourje.examples.npb3.impl IS)))

(config/clj-or-dcj)

;;;;
;;;; Specification
;;;;

(s/defrole ::master)
(s/defrole ::worker)

(s/defsession ::is [k]
  (s/cat (s/* (s/par-every [i (range k)]
                (s/cat (s/-->> discourje.examples.npb3.impl.ISThreads.RankMessage ::master (::worker i))
                       (s/-->> discourje.examples.npb3.impl.DoneMessage (::worker i) ::master))))
         (s/par-every [i (range k)]
           (s/cat (s/-->> discourje.examples.npb3.impl.ExitMessage ::master (::worker i))
                  (s/-->> discourje.examples.npb3.impl.DoneMessage (::worker i) ::master)))
         (s/par (s/par-every [i (range k)]
                  (s/close ::master (::worker i)))
                (s/par-every [i (range k)]
                  (s/close (::worker i) ::master)))))

;;;;
;;;; Implementation
;;;;

(let [input config/*input*
      resolution (:resolution input)
      secs (:secs input)
      k (:k input)
      class (:class input)
      verbose (:verbose input)]

  (let [;; Start timer
        begin (System/nanoTime)
        deadline (+ begin (* secs 1000 1000 1000))

        ;; Configure
        _ (do (Config/verbose verbose)
              (case config/*lib*
                :clj (Config/clj)
                :dcj (Config/dcj)
                :dcj-nil (Config/dcjNil)))

        ;; Run
        output (loop [not-done true
                      timer (timer/timer resolution)]
                 (if not-done
                   (do (IS/main (into-array String [(str "np=" k) (str "CLASS=" class)]))
                       (recur (< (System/nanoTime) deadline) (timer/tick timer)))
                   (timer/report timer)))

        ;; Stop timer
        end (System/nanoTime)]

    (Config/verbose true)
    (set! config/*output* output)
    (set! config/*time* (- end begin))))