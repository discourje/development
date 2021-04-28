(ns discourje.core.spec.lts
  (:require [clojure.set :refer [union]]
            [clojure.java.shell :refer [sh]]
            [discourje.core.spec.interp :as interp])
  (:import (java.util.function Function Predicate)
           (discourje.core.lts Action Action$Type State States LTS LTSs)))

;;;;
;;;; Actions
;;;;

(defn- action-type-keyword-to-enum [keyword]
  {:pre [(keyword? keyword)]}
  (case keyword
    :sync Action$Type/SYNC
    :send Action$Type/SEND
    :receive Action$Type/RECEIVE
    :close Action$Type/CLOSE
    (throw (Exception.))))

(defn action [interp-action]
  {:pre [(interp/action? interp-action)]}
  (Action. (:name interp-action)
           (action-type-keyword-to-enum (:type interp-action))
           (reify Predicate (test [_ message] ((:predicate interp-action) message)))
           (:sender interp-action)
           (:receiver interp-action)))

;;;;
;;;; States
;;;;

(defn expand-then-perform! [source-states type message sender receiver]
  (States/expandThenPerform source-states
                            (action-type-keyword-to-enum type)
                            message
                            sender
                            receiver))

;;;;
;;;; LTSs
;;;;

(defn lts? [x]
  (= (type x) LTS))

(defn lts [ast & {:keys [on-the-fly history]
                  :or   {on-the-fly false, history false}}]
  (let [initial (if history [ast []] ast)
        expander (if history
                   (reify
                     Function
                     (apply [_ [ast hist]]
                       (let [successors (interp/successors-with-hist ast hist)]
                         (zipmap (map #(action (interp/action %)) (keys successors))
                                 (vals successors)))))
                   (reify
                     Function
                     (apply [_ ast]
                       (let [successors (interp/successors ast)]
                         (zipmap (map #(action (interp/action %)) (keys successors))
                                 (vals successors))))))
        lts (LTS. #{initial} expander)]
    (if (not on-the-fly)
      (.expandRecursively lts))
    lts))

(defn initial-states [lts]
  (.getInitialStates lts))

(defn channels [lts]
  (into (sorted-set) (reduce clojure.set/union
                             (map (fn [^State s]
                                    (reduce clojure.set/union
                                            (map (fn [a] #{[(.getSender a) (.getReceiver a)]})
                                                 (.getActions (.getTransitionsOrNull s)))))
                                  (.getStates lts)))))

(defn roles [lts]
  (reduce clojure.set/union
          (map (fn [^State s]
                 (reduce clojure.set/union
                         (map (fn [a] #{(.getSender a) (.getReceiver a)})
                              (.getActions (.getTransitionsOrNull s)))))
               (.getStates lts))))

(defn bisimilar? [lts1 lts2]
  (LTSs/bisimilar lts1 lts2))

(defn not-bisimilar? [lts1 lts2]
  (not (bisimilar? lts1 lts2)))