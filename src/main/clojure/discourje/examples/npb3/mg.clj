(ns discourje.examples.npb3.mg
  (:require [clojure.core.async]
            [discourje.core.async]
            [discourje.core.spec :as s]
            [discourje.core.lint :as l]
            [discourje.examples.config :as config])
  (:import (discourje.examples.npb3 Config)
           (discourje.examples.npb3.impl MG)))

;;;;;
;;;;; Specification
;;;;;

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

;(s/defsession ::mg [k]
;  (s/cat (s/* (s/alt (s/cat (s/cat-every [i (range k)]
;                              (s/-->> discourje.examples.npb3.impl.MGThreads.InterpMessage ::master (::interp i)))
;                            (s/cat-every [i (range k)]
;                              (s/-->> discourje.examples.npb3.impl.DoneMessage (::interp i) ::master)))
;                     (s/cat (s/cat-every [i (range k)]
;                              (s/-->> discourje.examples.npb3.impl.MGThreads.PsinvMessage ::master (::psinv i)))
;                            (s/cat-every [i (range k)]
;                              (s/-->> discourje.examples.npb3.impl.DoneMessage (::psinv i) ::master)))
;                     (s/cat (s/cat-every [i (range k)]
;                              (s/-->> discourje.examples.npb3.impl.MGThreads.RprjMessage ::master (::rprj i)))
;                            (s/cat-every [i (range k)]
;                              (s/-->> discourje.examples.npb3.impl.DoneMessage (::rprj i) ::master)))
;                     (s/cat (s/cat-every [i (range k)]
;                              (s/-->> discourje.examples.npb3.impl.MGThreads.ResidMessage ::master (::resid i)))
;                            (s/cat-every [i (range k)]
;                              (s/-->> discourje.examples.npb3.impl.DoneMessage (::resid i) ::master)))))
;         (s/cat-every [i (range k)]
;           (s/cat (s/cat (s/-->> discourje.examples.npb3.impl.ExitMessage ::master (::interp i))
;                         (s/-->> discourje.examples.npb3.impl.DoneMessage (::interp i) ::master))
;                  (s/cat (s/-->> discourje.examples.npb3.impl.ExitMessage ::master (::psinv i))
;                         (s/-->> discourje.examples.npb3.impl.DoneMessage (::psinv i) ::master))
;                  (s/cat (s/-->> discourje.examples.npb3.impl.ExitMessage ::master (::rprj i))
;                         (s/-->> discourje.examples.npb3.impl.DoneMessage (::rprj i) ::master))
;                  (s/cat (s/-->> discourje.examples.npb3.impl.ExitMessage ::master (::resid i))
;                         (s/-->> discourje.examples.npb3.impl.DoneMessage (::resid i) ::master))))
;         (s/cat (s/cat-every [i (range k)]
;                  (s/close ::master (::interp i)))
;                (s/cat-every [i (range k)]
;                  (s/close (::interp i) ::master))
;                (s/cat-every [i (range k)]
;                  (s/close ::master (::psinv i)))
;                (s/cat-every [i (range k)]
;                  (s/close (::psinv i) ::master))
;                (s/cat-every [i (range k)]
;                  (s/close ::master (::rprj i)))
;                (s/cat-every [i (range k)]
;                  (s/close (::rprj i) ::master))
;                (s/cat-every [i (range k)]
;                  (s/close ::master (::resid i)))
;                (s/cat-every [i (range k)]
;                  (s/close (::resid i) ::master)))))

(defn spec []
  (mg (:k config/*input*)))

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
          output (MG/main (into-array String [(str "np=" k) (str "CLASS=" class)]))]

      (Config/verbose true)
      (set! config/*output* output))))