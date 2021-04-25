(ns discourje.core.lint
  (:gen-class)
  (:refer-clojure :exclude [send and or not])
  (:require [discourje.core.spec :as s]
            [discourje.core.spec.ast :as ast]
            [discourje.core.spec.interp :as interp]
            [discourje.core.spec.lts :as lts]
            [discourje.core.spec.mcrl2 :as mcrl2])
  (:import (discourje.core.validation Model State)
           (discourje.core.validation.formulas CtlFormula CtlFormulas)
           (discourje.core.lts LTS)))

;;;;
;;;; ATOMS
;;;;

(def init (CtlFormulas/init))

(def fin (CtlFormulas/fin))

(defmacro send [sender receiver]
  (let [sender (s/desugared-role sender)
        receiver (s/desugared-role receiver)]
    `(CtlFormulas/send (interp/eval-role ~sender)
                       (interp/eval-role ~receiver))))

(defmacro receive [sender receiver]
  (let [sender (s/desugared-role sender)
        receiver (s/desugared-role receiver)]
    `(CtlFormulas/receive (interp/eval-role ~sender)
                          (interp/eval-role ~receiver))))

(defmacro close [sender receiver]
  (let [sender (s/desugared-role sender)
        receiver (s/desugared-role receiver)]
    `(CtlFormulas/close (interp/eval-role ~sender)
                        (interp/eval-role ~receiver))))

(defmacro act [role]
  (let [role (s/desugared-role role)]
    `(CtlFormulas/act (interp/eval-role ~role))))

;;;;
;;;; PROPOSITIONAL OPERATORS
;;;;

(defn and [& args]
  (CtlFormulas/and (into-array CtlFormula args)))

(defn or [& args]
  (CtlFormulas/or (into-array CtlFormula args)))

(defn not [arg]
  (CtlFormulas/not arg))

(defn implies [arg1 arg2]
  (CtlFormulas/implies arg1 arg2))

;;;;
;;;; TEMPORAL OPERATORS - FUTURE
;;;;

(defn AX [arg]
  (CtlFormulas/AX arg))

(defn AF [arg]
  (CtlFormulas/AF arg))

(defn AG [arg]
  (CtlFormulas/AG arg))

(defn AU [arg1 arg2]
  (CtlFormulas/AU arg1 arg2))

(defn EX [arg]
  (CtlFormulas/EX arg))

(defn EF [arg]
  (CtlFormulas/EF arg))

(defn EG [arg]
  (CtlFormulas/EG arg))

(defn EU [arg1 arg2]
  (CtlFormulas/EU arg1 arg2))

;;;;
;;;; TEMPORAL OPERATORS - PAST
;;;;

(defn AY [arg]
  (CtlFormulas/AY arg))

(defn AP [arg]
  (CtlFormulas/AP arg))

(defn AH [arg]
  (CtlFormulas/AH arg))

(defn AS [arg1 arg2]
  (CtlFormulas/AS arg1 arg2))

(defn EY [arg]
  (CtlFormulas/EY arg))

(defn EP [arg]
  (CtlFormulas/EP arg))

(defn EH [arg]
  (CtlFormulas/EH arg))

(defn ES [arg1 arg2]
  (CtlFormulas/ES arg1 arg2))

;;;;
;;;; GENERIC PROPERTIES
;;;;

(defn must-terminate []
  (AG (AF fin)))

(defn may-terminate []
  (AG (EF fin)))

(defn cant-terminate []
  (AG (not fin)))

(defn close-after-send [channels]
  (let [args (map (fn [[sender receiver]]
                    (let [x (eval `(send ~sender ~receiver))
                          y (eval `(close ~sender ~receiver))]
                      (AG (implies x (AF y)))))
                  channels)
        f (apply and args)]
    f))

(defn send-before-close [channels]
  (let [args (map (fn [[sender receiver]]
                    (let [x (eval `(send ~sender ~receiver))
                          y (eval `(close ~sender ~receiver))]
                      ;; Alternative: (AU (not y) (or fin x))
                      (AG (implies y (AP x)))))
                  channels)
        f (apply and args)]
    f))

(defn no-act-after-close [channels]
  (let [args (map (fn [[sender receiver]]
                    (let [x (eval `(send ~sender ~receiver))
                          y (eval `(close ~sender ~receiver))]
                      (AG (implies y (or fin (AX (AG (not (or x y)))))))))
                  channels)
        f (apply and args)]
    f))

(defn causality [roles]
  (let [args (remove nil?
                     (apply concat
                            (map (fn [p] (map (fn [q]
                                                (if (not= p q)
                                                  (let [act-p (eval `(act ~p))
                                                        act-q (eval `(act ~q))]
                                                    (AG (implies (EX (and act-p (EX act-q)))
                                                                 (EX (and act-q (EX act-p))))))))
                                              roles))
                                 roles)))
        f (apply and args)]
    f))

;;;;
;;;; API
;;;;

(defn check-all [ast-or-lts fmap & {:keys [engine justify]
                                    :or   {engine :dcj justify true}}]
  (if (= (type ast-or-lts) LTS)
    (condp = engine
      :dcj (let [m (Model. ^LTS ast-or-lts)]
             (into {} (map (fn [[fname f]]
                             (let [begin (System/nanoTime)]
                               (.label f m)
                               (let [i (.getLabelIndex m f)
                                     end (System/nanoTime)
                                     result (every? #(.hasLabel ^State % i) (.getInitialStates m))
                                     time (long (/ (- end begin) 1000000))]
                                 (if (clojure.core/or result (clojure.core/not justify))
                                   [fname {:result result
                                           :time   time
                                           :engine engine}]
                                   [fname {:result   result
                                           :evidence (str (.getCounterexample f m))
                                           :time     time
                                           :engine   engine}]))))
                           fmap)))
      :mcrl2 (into {} (map (fn [[fname f]]
                             (let [begin (System/nanoTime)
                                   result (try
                                            (:f @(mcrl2/lts2pbes-pbes2bool ast-or-lts {:f (.toMCRL2 f)}))
                                            (catch Exception _))
                                   end (System/nanoTime)
                                   time (long (/ (- end begin) 1000000))]
                               [fname {:result result
                                       :time   time
                                       :engine engine}]))
                           fmap)))
    (check-all (lts/lts ast-or-lts) fmap :engine engine :justify justify)))

(defn check-one [ast-or-lts f & {:keys [engine justify]
                                 :or   {engine :dcj justify true}}]
  (:f (check-all ast-or-lts {:f f} :engine engine :justify justify)))

(defn lint [ast-or-lts & {:keys [engine justify]
                          :or   {engine :dcj justify true}}]
  (if (= (type ast-or-lts) LTS)
    (let [channels (lts/channels ast-or-lts)
          roles (lts/roles ast-or-lts)
          fmap {:must-terminate     (must-terminate)
                :may-terminate      (may-terminate)
                :cant-terminate     (cant-terminate)
                :close-after-send   (close-after-send channels)
                :send-before-close  (send-before-close channels)
                :no-act-after-close (no-act-after-close channels)
                :causality          (causality roles)}]
      (check-all ast-or-lts fmap :engine engine :justify justify))
    (lint (lts/lts ast-or-lts) :engine engine :justify justify)))