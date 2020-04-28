(ns discourje.spec.lts
  (:require [clojure.set :refer [union]]
            [clojure.java.shell :refer [sh]]
            [discourje.spec.interp :as interp])
  (:import (java.util.function Function Predicate)
           (discourje.lts Action Action$Type States LTS LTSs)))

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

(defn action [name type predicate sender receiver]
  {:pre [(string? name)
         (keyword? type)
         (fn? predicate)
         (string? sender)
         (string? receiver)]}
  (Action. name
           (action-type-keyword-to-enum type)
           (reify Predicate (test [_ message] (predicate message)))
           sender
           receiver))

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

(defn lts
  ([ast]
   (lts ast true))
  ([ast expand-recursively]
   (let [lts (LTS. #{ast}
                   (reify
                     Function
                     (apply [_ ast] (interp/successors ast action))))]
     (if expand-recursively
       (.expandRecursively lts))
     lts)))

(defn initial-states [lts]
  (.getInitialStates lts))

(defn bisimilar? [lts1 lts2]
  (LTSs/bisimilar lts1 lts2))

(defn not-bisimilar? [lts1 lts2]
  (not (bisimilar? lts1 lts2)))

(defn ltsgraph [lts mcrl2-root-dir tmp-file]
  (spit tmp-file (.toString lts))
  (future (clojure.java.shell/sh (str mcrl2-root-dir "/bin/ltsgraph") tmp-file)))