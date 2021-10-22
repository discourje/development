(ns discourje.examples.npb3.is
  (:require [clojure.core.async]
            [discourje.core.async]
            [discourje.core.spec :as s]
            [discourje.core.lint :as l]
            [discourje.examples.config :as config])
  (:import (discourje.examples.npb3 Config)
           (discourje.examples.npb3.impl IS)))

;;;;;
;;;;; Specification
;;;;;

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

;(s/defsession ::is [k]
;  (s/cat (s/* (s/cat (s/cat-every [i (range k)]
;                       (s/-->> discourje.examples.npb3.impl.ISThreads.RankMessage ::master (::worker i)))
;                     (s/cat-every [i (range k)]
;                       (s/-->> discourje.examples.npb3.impl.DoneMessage (::worker i) ::master))))
;         (s/cat-every [i (range k)]
;           (s/cat (s/-->> discourje.examples.npb3.impl.ExitMessage ::master (::worker i))
;                  (s/-->> discourje.examples.npb3.impl.DoneMessage (::worker i) ::master)))
;         (s/cat (s/cat-every [i (range k)]
;                  (s/close ::master (::worker i)))
;                (s/cat-every [i (range k)]
;                  (s/close (::worker i) ::master)))))

(defn spec []
  (is (:k config/*input*)))

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
          output (IS/main (into-array String [(str "np=" k) (str "CLASS=" class)]))]

      (Config/verbose true)
      (set! config/*output* output))))