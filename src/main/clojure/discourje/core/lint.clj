(ns discourje.core.lint
  (:gen-class)
  (:refer-clojure :exclude [send and or not])
  (:require [discourje.core.spec :as s]
            [discourje.core.spec.interp :as interp]
            [discourje.core.spec.lts :as lts]
            [discourje.core.spec.mcrl2 :as mcrl2])
  (:import (discourje.core.ctl Formula Formulas Model State)
           (discourje.core.lts Action Action$Type LTS)))

;;;;
;;;; ATOMS
;;;;

(def init (Formulas/init))

(def fin (Formulas/fin))

(defmacro handshake [sender receiver]
  (let [sender (if sender (s/desugared-role sender))
        receiver (if receiver (s/desugared-role receiver))]
    `(Formulas/handshake (if ~sender (interp/eval-role ~sender))
                         (if ~receiver (interp/eval-role ~receiver)))))
(defmacro send [sender receiver]
  (let [sender (if sender (s/desugared-role sender))
        receiver (if receiver (s/desugared-role receiver))]
    `(Formulas/send (if ~sender (interp/eval-role ~sender))
                    (if ~receiver (interp/eval-role ~receiver)))))

(defmacro receive [sender receiver]
  (let [sender (if sender (s/desugared-role sender))
        receiver (if receiver (s/desugared-role receiver))]
    `(Formulas/receive (if ~sender (interp/eval-role ~sender))
                       (if ~receiver (interp/eval-role ~receiver)))))

(defmacro close [sender receiver]
  (let [sender (if sender (s/desugared-role sender))
        receiver (if receiver (s/desugared-role receiver))]
    `(Formulas/close (if ~sender (interp/eval-role ~sender))
                     (if ~receiver (interp/eval-role ~receiver)))))

(defmacro act [role]
  (let [role (if role (s/desugared-role role))]
    `(Formulas/act (if ~role (interp/eval-role ~role)))))

;;;;
;;;; PROPOSITIONAL OPERATORS
;;;;

(defn and [& args]
  (Formulas/and (into-array Formula args)))

(defn or [& args]
  (Formulas/or (into-array Formula args)))

(defn not [arg]
  (Formulas/not arg))

(defn implies [arg1 arg2]
  (Formulas/implies arg1 arg2))

;;;;
;;;; TEMPORAL OPERATORS - FUTURE
;;;;

(defn AX [arg]
  (Formulas/AX arg))

(defn AF [arg]
  (Formulas/AF arg))

(defn AG [arg]
  (Formulas/AG arg))

(defn AU [arg1 arg2]
  (Formulas/AU arg1 arg2))

(defn EX [arg]
  (Formulas/EX arg))

(defn EF [arg]
  (Formulas/EF arg))

(defn EG [arg]
  (Formulas/EG arg))

(defn EU [arg1 arg2]
  (Formulas/EU arg1 arg2))

;;;;
;;;; TEMPORAL OPERATORS - PAST
;;;;

(defn AY [arg]
  (Formulas/AY arg))

(defn AP [arg]
  (Formulas/AP arg))

(defn AH [arg]
  (Formulas/AH arg))

(defn AS [arg1 arg2]
  (Formulas/AS arg1 arg2))

(defn EY [arg]
  (Formulas/EY arg))

(defn EP [arg]
  (Formulas/EP arg))

(defn EH [arg]
  (Formulas/EH arg))

(defn ES [arg1 arg2]
  (Formulas/ES arg1 arg2))

;;;;
;;;; CUSTOM
;;;;

(def diamond
  (let [active-roles (fn [^Action a]
                       (condp = (.getType a)
                         Action$Type/SYNC #{(.getSender a) (.getReceiver a)}
                         Action$Type/SEND #{(.getSender a)}
                         Action$Type/RECEIVE #{(.getReceiver a)}
                         Action$Type/CLOSE #{(.getSender a)}))
        disjoint-active-roles (fn [^Action a1 ^Action a2]
                                (let [active1 (active-roles a1)
                                      active2 (active-roles a2)
                                      disjoint (empty? (clojure.set/intersection active1 active2))]
                                  disjoint))]

    (reify Formula
      (isTemporal [_] false)
      (label [this model]
        (if-not (.isLabelledBy model this)
          (let [i (.setLabelledBy model this)]
            (doseq [s ^State (.getStates model)]
              (let [results (identity

                              ;; For every predecessor of s...
                              (for [prev ^State (.getPreviousStates s)]

                                ;; For every predecessor of prev...
                                (for [prevprev ^State (.getPreviousStates prev)]
                                  (if (disjoint-active-roles (.getAction s) (.getAction prev))

                                    ;; For some successor of prevprev...
                                    (if (loop [nexts (.getNextStates prevprev)]
                                          (when-let [next (first nexts)]

                                            ;; For some successor of next...
                                            (if (loop [nextnexts (.getNextStates next)]
                                                  (when-let [nextnext (first nextnexts)]

                                                    ;; Diamond
                                                    (if (clojure.core/and (= (.getAction s) (.getAction next))
                                                                          (= (.getAction prev) (.getAction nextnext))
                                                                          (= (.getState s) (.getState nextnext)))
                                                      true
                                                      (recur (rest nextnexts)))))
                                              true
                                              (recur (rest nexts)))))
                                      true)
                                    true))))]
                (if (clojure.core/and (not-any? empty? results)
                                      (not= '() results)
                                      (every? true? (flatten results)))
                  (.addLabel s i)))))))
      (extractWitness [_ _ _] [[]])
      )))

;;;;
;;;; GENERIC PROPERTIES
;;;;

(defn must-terminate []
  (AF fin))

(defn may-terminate []
  (AG (EF fin)))

(defn cant-terminate []
  (AG (not fin)))

(defn close-after-send [channels]
  (let [args (map (fn [[p q]]
                    (let [handshake-pq (eval `(handshake ~p ~q))
                          send-p (eval `(send ~p ~q))
                          close-p (eval `(close ~p ~q))]
                      (AG (implies (or handshake-pq send-p) (AF close-p)))))
                  channels)
        f (apply and args)]
    f))

(defn send-before-close [channels]
  (let [args (map (fn [[p q]]
                    (let [handshake-pq (eval `(handshake ~p ~q))
                          send-p (eval `(send ~p ~q))
                          close-p (eval `(close ~p ~q))]
                      (AG (implies close-p (AP (or handshake-pq send-p))))))
                  channels)
        f (apply and args)]
    f))

(defn no-act-after-close [channels]
  (let [args (map (fn [[p q]]
                    (let [handshake-pq (eval `(handshake ~p ~q))
                          send-p (eval `(send ~p ~q))
                          close-p (eval `(close ~p ~q))]
                      (AG (implies close-p (or fin (AX (AG (not (or handshake-pq send-p close-p)))))))))
                  channels)
        f (apply and args)]
    f))

(defn causality [channels]
  (let [args (map (fn [[p q]]
                    (let [handshake-pq (eval `(handshake ~p ~q))
                          send-p (eval `(send ~p ~q))
                          close-p (eval `(close ~p ~q))
                          act-p (eval `(act ~p))
                          act-q (eval `(act ~q))]

                      (AG (and (implies handshake-pq
                                        (or (AY (or init act-p act-q))
                                            diamond))
                               (implies (or send-p close-p)
                                        (or (AY (or init act-p))
                                            diamond))))))
                  channels)
        f (apply and args)]
    f))

;;;;
;;;; API
;;;;

(def ^:dynamic *engine* :dcj)
(def ^:dynamic *witness* true)
(def ^:dynamic *include* #{:must-terminate
                           :may-terminate
                           :cant-terminate
                           :close-after-send
                           :send-before-close
                           :no-act-after-close
                           :causality})
(def ^:dynamic *exclude* #{})

(defn check-all [ast-or-lts fmap & {:keys [engine witness]
                                    :or   {engine *engine* witness *witness*}}]
  (if (= (type ast-or-lts) LTS)
    (condp = engine
      :dcj (let [m (Model. ^LTS ast-or-lts)]
             (into (sorted-map) (map (fn [[fname f]]
                                       (let [begin (System/nanoTime)]
                                         (.calculateLabels m f)
                                         (let [end (System/nanoTime)
                                               verdict (every? #(.hasLabel m % f) (.getInitialStates m))
                                               time (long (/ (- end begin) 1000000))]
                                           (if (clojure.core/or verdict (clojure.core/not witness))
                                             [fname {:verdict verdict
                                                     :time    time}]
                                             [fname {:verdict verdict
                                                     :witness (map #(str %) (flatten (seq (map #(seq %) (.extractWitness f m)))))
                                                     :time    time}]))))
                                     fmap)))

      :mcrl2 @(mcrl2/lts2pbes-pbes2bool ast-or-lts
                                        (into {} (map (fn [[fname f]] [fname (.toMCRL2 f)]) fmap)))

      :mcrl2-split @(mcrl2/lts2pbes-pbes2bool ast-or-lts
                                              (into {} (map (fn [[fname f]]
                                                              (loop [conjuncts (.split f)
                                                                     i 1
                                                                     m (sorted-map)]
                                                                (if (empty? conjuncts)
                                                                  m
                                                                  (recur (rest conjuncts)
                                                                         (inc i)
                                                                         (assoc m (keyword (str (name fname) "-" i))
                                                                                  (.toMCRL2 (first conjuncts)))))))
                                                            fmap))))
    (check-all (lts/lts ast-or-lts) fmap :engine engine :witness witness)))

(defn check-one [ast-or-lts f & {:keys [engine witness]
                                 :or   {engine *engine* witness *witness*}}]
  (:f (check-all ast-or-lts {:f f} :engine engine :witness witness)))

(defn lint [ast-or-lts & {:keys [engine witness include exclude]
                          :or   {engine  *engine*
                                 witness *witness*
                                 include *include*
                                 exclude *exclude*}}]
  (if (= (type ast-or-lts) LTS)
    (let [channels (lts/channels ast-or-lts)
          fmap {:must-terminate     (must-terminate)
                :may-terminate      (may-terminate)
                :cant-terminate     (cant-terminate)
                :close-after-send   (close-after-send channels)
                :send-before-close  (send-before-close channels)
                :no-act-after-close (no-act-after-close channels)
                :causality          (causality channels)}
          fmap (select-keys fmap (clojure.set/difference include exclude))]
      (check-all ast-or-lts fmap :engine engine :witness witness))
    (lint (lts/lts ast-or-lts)
          :engine engine
          :witness witness
          :include include
          :exclude exclude)))