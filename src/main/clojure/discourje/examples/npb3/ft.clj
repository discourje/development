(ns discourje.examples.npb3.ft
  (:require [clojure.core.async]
            [discourje.core.async]
            [discourje.core.spec :as s]
            [discourje.core.lint :as l]
            [discourje.examples.config :as config])
  (:import (discourje.examples.npb3 Config)
           (discourje.examples.npb3.impl FT)))

;;;;;
;;;;; Specification
;;;;;

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

;(s/defsession ::ft [k]
;  (s/cat (s/* (s/alt (s/cat (s/cat-every [i (range k)]
;                              (s/-->> discourje.examples.npb3.impl.FTThreads.EvolveMessage ::master (::evolve i)))
;                            (s/cat-every [i (range k)]
;                              (s/-->> discourje.examples.npb3.impl.DoneMessage (::evolve i) ::master)))
;                     (s/cat (s/cat-every [i (range k)]
;                              (s/-->> discourje.examples.npb3.impl.FTThreads.FFTMessage ::master (::fft i)))
;                            (s/cat-every [i (range k)]
;                              (s/-->> discourje.examples.npb3.impl.DoneMessage (::fft i) ::master)))
;                     (s/cat (s/cat-every [i (range k)]
;                              (s/-->> discourje.examples.npb3.impl.FTThreads.FFTSetVariablesMessage ::master (::fft i)))
;                            (s/cat-every [i (range k)]
;                              (s/-->> discourje.examples.npb3.impl.DoneMessage (::fft i) ::master)))))
;         (s/cat-every [i (range k)]
;           (s/cat (s/cat (s/-->> discourje.examples.npb3.impl.ExitMessage ::master (::evolve i))
;                         (s/-->> discourje.examples.npb3.impl.DoneMessage (::evolve i) ::master))
;                  (s/cat (s/-->> discourje.examples.npb3.impl.ExitMessage ::master (::fft i))
;                         (s/-->> discourje.examples.npb3.impl.DoneMessage (::fft i) ::master))))
;         (s/cat (s/cat-every [i (range k)]
;                  (s/close ::master (::evolve i)))
;                (s/cat-every [i (range k)]
;                  (s/close (::evolve i) ::master))
;                (s/cat-every [i (range k)]
;                  (s/close ::master (::fft i)))
;                (s/cat-every [i (range k)]
;                  (s/close (::fft i) ::master)))))

(defn spec []
  (ft (:k config/*input*)))

(when (some? config/*lint*)
  (set! config/*output* (l/lint (spec))))

;;;;;
;;;;; Implementation
;;;;;

(config/clj-or-dcj)

(when (some? config/*run*)
  (let [input config/*input*
        k (:k input)
        class (:class input)
        verbose (:verbose input)]

    (let [;; Configure
          _ (do (Config/verbose verbose)
                (case config/*run*
                  :clj (Config/clj)
                  :dcj (Config/dcj)
                  :dcj-nil (Config/dcjNil)))

          ;; Run
          output (FT/main (into-array String [(str "np=" k) (str "CLASS=" class)]))]

      (Config/verbose true)
      (set! config/*output* output))))