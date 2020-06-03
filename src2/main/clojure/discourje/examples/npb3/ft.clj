(ns discourje.examples.npb3.ft
  (:require [clojure.core.async]
            [discourje.core.async]
            [discourje.core.spec :as s]
            [discourje.examples.config :as config]
            [discourje.examples.timer :as timer])
  (:import (discourje.examples.npb3 Config)
           (discourje.examples.npb3.impl FT)))

(config/clj-or-dcj)

;;;;
;;;; Specification
;;;;

(s/defrole ::master)
(s/defrole ::evolve)
(s/defrole ::fft)

(s/defsession ::ft [k]
  (s/cat (s/* (s/alt (s/par-every [i (range k)]
                       (s/cat (s/-->> discourje.examples.npb3.impl.FTThreads.EvolveMessage ::master (::evolve i))
                              (s/-->> discourje.examples.npb3.impl.DoneMessage (::evolve i) ::master)))
                     (s/par-every [i (range k)]
                       (s/cat (s/-->> discourje.examples.npb3.impl.FTThreads.FFTMessage ::master (::fft i))
                              (s/-->> discourje.examples.npb3.impl.DoneMessage (::fft i) ::master)))
                     (s/par-every [i (range k)]
                       (s/cat (s/-->> discourje.examples.npb3.impl.FTThreads.FFTSetVariablesMessage ::master (::fft i))
                              (s/-->> discourje.examples.npb3.impl.DoneMessage (::fft i) ::master)))))
         (s/par-every [i (range k)]
           (s/par (s/cat (s/-->> discourje.examples.npb3.impl.ExitMessage ::master (::evolve i))
                         (s/-->> discourje.examples.npb3.impl.DoneMessage (::evolve i) ::master))
                  (s/cat (s/-->> discourje.examples.npb3.impl.ExitMessage ::master (::fft i))
                         (s/-->> discourje.examples.npb3.impl.DoneMessage (::fft i) ::master))))
         (s/par (s/par-every [i (range k)]
                  (s/close ::master (::evolve i)))
                (s/par-every [i (range k)]
                  (s/close (::evolve i) ::master))
                (s/par-every [i (range k)]
                  (s/close ::master (::fft i)))
                (s/par-every [i (range k)]
                  (s/close (::fft i) ::master)))))

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
                   (do (FT/main (into-array String [(str "np=" k) (str "CLASS=" class)]))
                       (recur (< (System/nanoTime) deadline) (timer/tick timer)))
                   (timer/report timer)))

        ;; Stop timer
        end (System/nanoTime)]

    (Config/verbose true)
    (set! config/*output* output)
    (set! config/*time* (- end begin))))