(ns discourje.core.ctl
  (:gen-class)
  (:refer-clojure :exclude [send and or not])
  (:require [clojure.walk :as w]
            [discourje.core.spec :as s]
            [discourje.core.spec.ast :as ast]
            [discourje.core.spec.interp :as interp]
            [discourje.core.spec.lts :as lts])
  (:import (discourje.core.validation ModelChecker)
           (discourje.core.validation.formulas CtlFormula CtlFormulas)))

(defn check [spec f]
  (ModelChecker/check (lts/lts spec) f))

;;;;
;;;; ATOMS
;;;;

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
