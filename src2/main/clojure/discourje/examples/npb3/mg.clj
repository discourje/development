(ns discourje.examples.npb3.mg
  (:require [clojure.core.async]
            [discourje.core.async]
            [discourje.core.spec :as s]
            [discourje.examples.config :as config]
            [discourje.examples.timer :as timer])
  (:import (discourje.examples.npb3 Config)
           (discourje.examples.npb3.impl MG)))

(config/clj-or-dcj)

;;;;
;;;; Specification
;;;;

(s/defrole ::master)
(s/defrole ::interp)
(s/defrole ::psinv)
(s/defrole ::rprj)
(s/defrole ::resid)

(s/defsession ::mg [k]
  (s/cat (s/* (s/alt (s/par-every [i (range k)]
                       (s/cat (s/-->> discourje.examples.npb3.impl.MGThreads.InterpMessage ::master (::interp i))
                              (s/-->> discourje.examples.npb3.impl.DoneMessage (::interp i) ::master)))
                     (s/par-every [i (range k)]
                       (s/cat (s/-->> discourje.examples.npb3.impl.MGThreads.PsinvMessage ::master (::psinv i))
                              (s/-->> discourje.examples.npb3.impl.DoneMessage (::psinv i) ::master)))
                     (s/par-every [i (range k)]
                       (s/cat (s/-->> discourje.examples.npb3.impl.MGThreads.RprjMessage ::master (::rprj i))
                              (s/-->> discourje.examples.npb3.impl.DoneMessage (::rprj i) ::master)))
                     (s/par-every [i (range k)]
                       (s/cat (s/-->> discourje.examples.npb3.impl.MGThreads.ResidMessage ::master (::resid i))
                              (s/-->> discourje.examples.npb3.impl.DoneMessage (::resid i) ::master)))))
         (s/par-every [i (range k)]
           (s/par (s/cat (s/-->> discourje.examples.npb3.impl.ExitMessage ::master (::interp i))
                         (s/-->> discourje.examples.npb3.impl.DoneMessage (::interp i) ::master))
                  (s/cat (s/-->> discourje.examples.npb3.impl.ExitMessage ::master (::psinv i))
                         (s/-->> discourje.examples.npb3.impl.DoneMessage (::psinv i) ::master))
                  (s/cat (s/-->> discourje.examples.npb3.impl.ExitMessage ::master (::rprj i))
                         (s/-->> discourje.examples.npb3.impl.DoneMessage (::rprj i) ::master))
                  (s/cat (s/-->> discourje.examples.npb3.impl.ExitMessage ::master (::resid i))
                         (s/-->> discourje.examples.npb3.impl.DoneMessage (::resid i) ::master))))
         (s/par (s/par-every [i (range k)]
                  (s/close ::master (::interp i)))
                (s/par-every [i (range k)]
                  (s/close (::interp i) ::master))
                (s/par-every [i (range k)]
                  (s/close ::master (::psinv i)))
                (s/par-every [i (range k)]
                  (s/close (::psinv i) ::master))
                (s/par-every [i (range k)]
                  (s/close ::master (::rprj i)))
                (s/par-every [i (range k)]
                  (s/close (::rprj i) ::master))
                (s/par-every [i (range k)]
                  (s/close ::master (::resid i)))
                (s/par-every [i (range k)]
                  (s/close (::resid i) ::master)))))

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
                   (do (MG/main (into-array String [(str "np=" k) (str "CLASS=" class)]))
                       (recur (< (System/nanoTime) deadline) (timer/tick timer)))
                   (timer/report timer)))

        ;; Stop timer
        end (System/nanoTime)]

    (Config/verbose true)
    (set! config/*output* output)
    (set! config/*time* (- end begin))))