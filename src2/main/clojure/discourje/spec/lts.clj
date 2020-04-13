(ns discourje.spec.lts
  (:require [clojure.set :refer [union]]
            [discourje.spec.interp :as interp])
  (:import (java.util.function Function Predicate)
           (discourje.spec.lts Action Action$Type State LTS LTSs)))

;;;;
;;;; Action
;;;;

(defn action [name type predicate sender receiver]
  {:pre [(string? name)
         (keyword? type)
         (fn? predicate)
         (string? sender)
         (string? receiver)]}
  (Action. name
           (case type
             :sync Action$Type/SYNC
             :send Action$Type/SEND
             :receive Action$Type/RECEIVE
             :close Action$Type/CLOSE
             (throw (Exception.)))
           (reify Predicate (test [_ message] (predicate message)))
           sender
           receiver))

;;;;
;;;; LTS
;;;;

(defn lts [ast]
  (LTS. #{ast}
        (reify
          Function
          (apply [_ ast] (interp/successors ast action)))))

(defn lts? [x]
  (= (type x) LTS))

(defn expandRecursively!
  ([lts]
   (.expandRecursively lts))
  ([lts bound]
   (.expandRecursively lts bound)))

(defn initial-states [lts]
  (.getInitialStates lts))

(defn bisimilar? [lts1 lts2]
  (LTSs/bisimilar lts1 lts2))

;;;;
;;;; TODO: Move this to Java
;;;;

(defn- expand-and-perform! [source-states type message sender receiver]
  (loop [todo source-states
         result {}]
    (if (empty? todo)
      result
      (let [source-state (first todo)
            _ (.expand source-state)
            target-states (set (.perform (.getTransitionsOrNull source-state)
                                         type message sender receiver))]
        (if (empty? target-states)
          {}
          (recur (rest todo) (clojure.set/union result target-states)))))))

(defn expand-and-sync! [source-states message sender receiver]
  (expand-and-perform! source-states Action$Type/SYNC message sender receiver))
(defn expand-and-send! [source-states message sender receiver]
  (expand-and-perform! source-states Action$Type/SEND message sender receiver))
(defn expand-and-receive! [source-states sender receiver]
  (expand-and-perform! source-states Action$Type/RECEIVE nil sender receiver))
(defn expand-and-close! [source-states sender receiver]
  (expand-and-perform! source-states Action$Type/CLOSE nil sender receiver))

