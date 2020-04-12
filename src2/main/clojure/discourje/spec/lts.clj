(ns discourje.spec.lts
  (:require [clojure.set :refer [rename-keys]]
            [discourje.spec.ast :as ast]
            [discourje.spec.interp :as interp])
  (:import (java.util.function Function Predicate)
           (discourje.spec.lts Action Action$Type State LTS LTSs)))

(defn eval-action [ast]
  (let [predicate (ast/eval-predicate (:predicate ast))
        sender (ast/eval-role (:sender ast))
        receiver (ast/eval-role (:receiver ast))
        type (cond (= (:type ast) :sync)
                   Action$Type/SYNC
                   (= (:type ast) :send)
                   Action$Type/SEND
                   (= (:type ast) :receive)
                   Action$Type/RECEIVE
                   (= (:type ast) :close)
                   Action$Type/CLOSE
                   :else (throw (Exception.)))
        name (str (cond (= (:type ast) :sync)
                        "‽"
                        (= (:type ast) :send)
                        "!"
                        (= (:type ast) :receive)
                        "?"
                        (= (:type ast) :close)
                        "C"
                        :else (throw (Exception.)))
                  "(" (if (or (= (:type ast) :sync) (= (:type ast) :send)) (str (:expr (:predicate ast)) ",") "") sender "," receiver ")")]
    {(Action. name type (reify Predicate (test [_ message] (predicate message))) sender receiver) [(ast/end)]}))

(defn lts [ast]
  (cond

    ;; Aldebaran
    (satisfies? ast/Aldebaran ast)
    (LTS. #{(:v0 ast)}
          (reify
            Function
            (apply [_ v]
              (let [m (get (:edges ast) v)
                    keys (keys m)
                    vals (map #(let [name %
                                     type (cond (= (first name) \‽)
                                                Action$Type/SYNC
                                                (= (first name) \!)
                                                Action$Type/SEND
                                                (= (first name) \?)
                                                Action$Type/RECEIVE
                                                (= (first name) \C)
                                                Action$Type/CLOSE
                                                :else (throw (Exception.)))
                                     predicate nil
                                     sender nil
                                     receiver nil]
                                 (Action. name type predicate sender receiver))
                              keys)]
                (if (nil? keys)
                  {}
                  (clojure.set/rename-keys m (zipmap keys vals)))))))

    ;; Discourje
    :else
    (LTS. #{ast}
          (reify
            Function
            (apply [_ ast] (interp/successors ast eval-action))))))

(defn lts? [x]
  (= (type x) LTS))

(defn expandRecursively!
  ([lts]
   (.expandRecursively lts))
  ([lts bound]
   (.expandRecursively lts bound)))

(defn initial-states [lts]
  (.getInitialStates lts))

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

(defn bisimilar? [lts1 lts2]
  (LTSs/bisimilar lts1 lts2))