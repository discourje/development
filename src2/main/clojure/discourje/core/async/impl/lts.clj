(ns discourje.core.async.impl.lts
  (:require [clojure.walk :as w]
            [clojure.java.shell :refer :all]
            [discourje.core.async.impl.ast :as ast])
  (:import (java.util.function Function Predicate)
           (discourje.core.async.impl.lts Send Receive Close)))

(defn- smash [spec]
  (if (and (vector? spec) (= (count spec) 1))
    (smash (first spec))
    spec))

(defn- smap [spec]
  (zipmap (:vars spec) (map eval (:exprs spec))))

(defn substitute [spec smap]
  ;(println spec)
  (cond

    ;; End
    (= (:type spec) :end)
    spec

    ;; Action
    (contains? ast/action-types (:type spec))
    (w/postwalk-replace smap spec)

    ;; Choice
    (= (:type spec) :choice)
    (ast/choice (mapv #(substitute % smap) (:branches spec)))

    ;; Parallel
    (= (:type spec) :parallel)
    (ast/parallel (mapv #(substitute % smap) (:branches spec)))

    ;; If
    (= (:type spec) :if)
    (ast/if-then-else (w/postwalk-replace smap (:condition spec))
                      (substitute (:branch1 spec) smap)
                      (substitute (:branch2 spec) smap))

    ;; Loop
    (= (:type spec) :loop)
    (ast/loop (:name spec)
              (:vars spec)
              (w/postwalk-replace smap (:exprs spec))
              (substitute (:body spec) (apply dissoc smap (:vars spec))))

    ;; Recur
    (= (:type spec) :recur)
    (ast/recur (:name spec)
               (w/postwalk-replace smap (:exprs spec)))

    ;; Sequence
    (vector? spec)
    (mapv #(substitute % smap) spec)

    ;; Application
    (coll? spec)
    (concat [(first spec)]
            (w/postwalk-replace smap (rest spec)))

    :else (throw (Exception.))))

(defn unfold [loop spec]
  ;(println "unfold: " (:type spec))
  (cond

    ;; End
    (= (:type spec) :end)
    spec

    ;; Action
    (contains? ast/action-types (:type spec))
    spec

    ;; Choice
    (= (:type spec) :choice)
    (ast/choice (mapv #(unfold loop %) (:branches spec)))

    ;; Parallel
    (= (:type spec) :parallel)
    (ast/parallel (mapv #(unfold loop %) (:branches spec)))

    ;; If
    (= (:type spec) :if)
    (ast/if-then-else (:condition spec)
                      (unfold loop (:branch1 spec))
                      (unfold loop (:branch2 spec)))

    ;; Loop
    (= (:type spec) :loop)
    (ast/loop (:name spec)
              (:vars spec)
              (:exprs spec)
              (if (= (:name loop) (:name spec)) (:body spec) (unfold loop (:body spec))))

    ;; Recur
    (= (:type spec) :recur)
    (if (= (:name loop) (:name spec))
      (ast/loop (:name loop) (:vars loop) (:exprs spec) (:body loop))
      spec)

    ;; Sequence
    (vector? spec)
    (mapv #(unfold loop %) spec)

    ;; Application
    (coll? spec)
    spec

    :else (throw (Exception.))))

(defn terminated? [spec]
  (cond

    ;; End
    (= (:type spec) :end)
    true

    ;; Action
    (contains? ast/action-types (:type spec))
    false

    ;; Choice
    (= (:type spec) :choice)
    (not (not-any? terminated? (:branches spec)))

    ;; Parallel
    (= (:type spec) :parallel)
    (every? terminated? (:branches spec))

    ;; If
    (= (:type spec) :if)
    (terminated? (if (eval (:condition spec)) (:branch1 spec) (:branch2 spec)))

    ;; Loop
    (= (:type spec) :loop)
    (terminated? (unfold spec (substitute (:body spec) (smap spec))))

    ;; Recur
    (= (:type spec) :recur)
    (throw (Exception.))

    ;; Sequence
    (vector? spec)
    (every? terminated? spec)

    ;; Application
    (coll? spec)
    (let [name (first spec)
          vals (map eval (rest spec))
          body (:body (get (get @ast/registry name) (count vals)))
          vars (:vars (get (get @ast/registry name) (count vals)))]
      (terminated? (substitute body (zipmap vars vals))))

    :else (throw (Exception.))))

(defn successors [spec]
  ;(println "successors: " (:type spec))
  (cond

    ;; End
    (= (:type spec) :end)
    {}

    ;; Action
    (contains? ast/action-types (:type spec))
    (let [role-fn (fn [role]
                    (if (coll? role)
                      (apply ((if (fn? (first role)) identity eval) (first role)) (map eval (rest role)))
                      (eval role)))
          sender (role-fn (:sender (:channel spec)))
          receiver (role-fn (:receiver (:channel spec)))
          predicate (:predicate spec)]
      (cond (= (:type spec) :send)
            {(reify
               Send
               (getSender [_] (.toString sender))
               (getReceiver [_] (.toString receiver))
               (getPredicate [_] (reify Predicate (test [this message] true)))
               Object
               (toString [_] (str "!(" sender "," receiver "," predicate ")")))
             (ast/end)}
            (= (:type spec) :receive)
            {(reify
               Receive
               (getSender [_] (.toString sender))
               (getReceiver [_] (.toString receiver))
               (getPredicate [_] (reify Predicate (test [this message] true)))
               Object
               (toString [_] (str "?(" sender "," receiver "," predicate ")")))
             (ast/end)}
            (= (:type spec) :close)
            {(reify
               Close
               (getSender [_] (.toString sender))
               (getReceiver [_] (.toString receiver))
               Object
               (toString [_] (str "C(" sender "," receiver ")")))
             (ast/end)}))

    ;; Choice
    (= (:type spec) :choice)
    (reduce merge (map successors (:branches spec)))

    ;; Parallel
    (= (:type spec) :parallel)
    (let [branches (:branches spec)]
      (loop [i 0
             result {}]
        (if (= i (count branches))
          result
          (let [branch (nth branches i)
                m (successors branch)
                f #(ast/parallel (into (subvec branches 0 i) (into [%] (subvec branches (inc i) (count branches)))))]
            (recur (inc i) (merge result (if (empty? m) {} (update-in m (keys m) f))))))))

    ;; If
    (= (:type spec) :if)
    (successors (if (eval (:condition spec)) (:branch1 spec) (:branch2 spec)))

    ;; Loop
    (= (:type spec) :loop)
    (successors (unfold spec (substitute (:body spec) (smap spec))))

    ;; Recur
    (= (:type spec) :recur)
    (throw (Exception.))

    ;; Sequence
    (vector? spec)
    (if (empty? spec)
      {}
      (if (terminated? (first spec))
        (successors (vec (rest spec)))
        (let [m (successors (first spec))]
          (update-in m (keys m)
                     #(smash (into (if (terminated? %) [] [%]) (rest spec)))))))

    ;; Application
    (coll? spec)
    (let [name (first spec)
          vals (mapv eval (rest spec))
          body (:body (get (get @ast/registry name) (count vals)))
          vars (:vars (get (get @ast/registry name) (count vals)))]
      (successors (substitute body (zipmap vars vals))))

    :else (throw (Exception.))))

(def expander
  (reify
    Function
    (apply [_ spec]
      ; (println (str "to expand: " spec))
      (let [m (successors spec)]
        ; (println (str "expanded: " m))
        m))))