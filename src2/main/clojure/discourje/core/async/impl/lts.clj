(ns discourje.core.async.impl.lts
  (:require [clojure.walk :as w]
            [clojure.java.shell :refer :all]
            [discourje.core.async.impl.ast :as ast])
  (:import (java.util.function Function Predicate)
           (discourje.core.async.impl.lts Action Send Receive Close LTS LTSs)))

(defn- smash [ast]
  (if (and (vector? ast) (= (count ast) 1))
    (smash (first ast))
    ast))

(defn- smap [ast]
  (zipmap (:vars ast) (map eval (:exprs ast))))

(defn substitute [ast smap]
  ;(println ast)
  (cond

    ;; End
    (= (:type ast) :end)
    ast

    ;; Action
    (contains? ast/action-types (:type ast))
    (w/postwalk-replace smap ast)

    ;; Choice
    (= (:type ast) :choice)
    (ast/choice (mapv #(substitute % smap) (:branches ast)))

    ;; Parallel
    (= (:type ast) :parallel)
    (ast/parallel (mapv #(substitute % smap) (:branches ast)))

    ;; If
    (= (:type ast) :if)
    (ast/if-then-else (w/postwalk-replace smap (:condition ast))
                      (substitute (:branch1 ast) smap)
                      (substitute (:branch2 ast) smap))

    ;; Loop
    (= (:type ast) :loop)
    (ast/loop (:name ast)
              (:vars ast)
              (w/postwalk-replace smap (:exprs ast))
              (substitute (:body ast) (apply dissoc smap (:vars ast))))

    ;; Recur
    (= (:type ast) :recur)
    (ast/recur (:name ast)
               (w/postwalk-replace smap (:exprs ast)))

    ;; Sequence
    (vector? ast)
    (mapv #(substitute % smap) ast)

    ;; Application
    (coll? ast)
    (concat [(first ast)]
            (w/postwalk-replace smap (rest ast)))

    :else (throw (Exception.))))

(defn unfold [loop ast]
  ;(println "unfold: " (:type ast))
  (cond

    ;; End
    (= (:type ast) :end)
    ast

    ;; Action
    (contains? ast/action-types (:type ast))
    ast

    ;; Choice
    (= (:type ast) :choice)
    (ast/choice (mapv #(unfold loop %) (:branches ast)))

    ;; Parallel
    (= (:type ast) :parallel)
    (ast/parallel (mapv #(unfold loop %) (:branches ast)))

    ;; If
    (= (:type ast) :if)
    (ast/if-then-else (:condition ast)
                      (unfold loop (:branch1 ast))
                      (unfold loop (:branch2 ast)))

    ;; Loop
    (= (:type ast) :loop)
    (ast/loop (:name ast)
              (:vars ast)
              (:exprs ast)
              (if (= (:name loop) (:name ast)) (:body ast) (unfold loop (:body ast))))

    ;; Recur
    (= (:type ast) :recur)
    (if (= (:name loop) (:name ast))
      (ast/loop (:name loop) (:vars loop) (:exprs ast) (:body loop))
      ast)

    ;; Sequence
    (vector? ast)
    (mapv #(unfold loop %) ast)

    ;; Application
    (coll? ast)
    ast

    :else (throw (Exception.))))

(defn terminated? [ast]
  (cond

    ;; End
    (= (:type ast) :end)
    true

    ;; Action
    (contains? ast/action-types (:type ast))
    false

    ;; Choice
    (= (:type ast) :choice)
    (not (not-any? terminated? (:branches ast)))

    ;; Parallel
    (= (:type ast) :parallel)
    (every? terminated? (:branches ast))

    ;; If
    (= (:type ast) :if)
    (terminated? (if (eval (:condition ast)) (:branch1 ast) (:branch2 ast)))

    ;; Loop
    (= (:type ast) :loop)
    (terminated? (unfold ast (substitute (:body ast) (smap ast))))

    ;; Recur
    (= (:type ast) :recur)
    (throw (Exception.))

    ;; Sequence
    (vector? ast)
    (every? terminated? ast)

    ;; Application
    (coll? ast)
    (let [name (first ast)
          vals (map eval (rest ast))
          body (:body (get (get @ast/registry name) (count vals)))
          vars (:vars (get (get @ast/registry name) (count vals)))]
      (terminated? (substitute body (zipmap vars vals))))

    :else (throw (Exception.))))

(defn eval-role [role]
  {:pre [(ast/role? role)]}
  (str (cond
         (string? (:name-expr role)) (:name-expr role)
         (keyword? (:name-expr role)) (ast/get-role-name (:name-expr role))
         :else (throw (Exception.)))
       (if (empty? (:index-exprs role))
         ""
         (mapv eval (:index-exprs role)))))

(defn successors [ast]
  ;(println "successors: " (:type ast))
  (cond

    ;; End
    (= (:type ast) :end)
    {}

    ;; Action
    (contains? ast/action-types (:type ast))
    (let [sender (eval-role (:sender ast))
          receiver (eval-role (:receiver ast))
          predicate (:predicate ast)]
      (cond (= (:type ast) :send)
            {(reify
               Send
               (getSender [_] (.toString sender))
               (getReceiver [_] (.toString receiver))
               (getPredicate [_] (reify Predicate (test [this message] true)))
               Object
               (equals [this o] (= (.toString this) (.toString o)))
               (hashCode [this] (.hashCode (.toString this)))
               (toString [_] (str "!(" sender "," receiver "," predicate ")")))
             (ast/end)}
            (= (:type ast) :receive)
            {(reify
               Receive
               (getSender [_] (.toString sender))
               (getReceiver [_] (.toString receiver))
               (getPredicate [_] (reify Predicate (test [this message] true)))
               Object
               (equals [this o] (= (.toString this) (.toString o)))
               (hashCode [this] (.hashCode (.toString this)))
               (toString [_] (str "?(" sender "," receiver "," predicate ")")))
             (ast/end)}
            (= (:type ast) :close)
            {(reify
               Close
               (getSender [_] (.toString sender))
               (getReceiver [_] (.toString receiver))
               Object
               (equals [this o] (= (.toString this) (.toString o)))
               (hashCode [this] (.hashCode (.toString this)))
               (toString [_] (str "C(" sender "," receiver ")")))
             (ast/end)}))

    ;; Choice
    (= (:type ast) :choice)
    (reduce merge (map successors (:branches ast)))

    ;; Parallel
    (= (:type ast) :parallel)
    (let [branches (:branches ast)]
      (loop [i 0
             result {}]
        (if (= i (count branches))
          result
          (let [branch (nth branches i)
                m (successors branch)
                f #(ast/parallel (into (subvec branches 0 i) (into [%] (subvec branches (inc i) (count branches)))))]
            (recur (inc i) (merge result (if (empty? m) {} (update-in m (keys m) f))))))))

    ;; If
    (= (:type ast) :if)
    (successors (if (eval (:condition ast)) (:branch1 ast) (:branch2 ast)))

    ;; Loop
    (= (:type ast) :loop)
    (successors (unfold ast (substitute (:body ast) (smap ast))))

    ;; Recur
    (= (:type ast) :recur)
    (throw (Exception.))

    ;; Sequence
    (vector? ast)
    (if (empty? ast)
      {}
      (if (terminated? (first ast))
        (successors (vec (rest ast)))
        (let [m (successors (first ast))]
          (update-in m (keys m)
                     #(smash (into (if (terminated? %) [] [%]) (rest ast)))))))

    ;; Application
    (coll? ast)
    (let [name (first ast)
          vals (mapv eval (rest ast))
          body (:body (get (get @ast/registry name) (count vals)))
          vars (:vars (get (get @ast/registry name) (count vals)))]
      (successors (substitute body (zipmap vars vals))))

    :else (throw (Exception.))))

(defn lts [ast expandRecursively]
  (cond

    ;; Aldebaran
    (satisfies? ast/Aldebaran ast)
    (LTS. (:v0 ast)
          (reify
            Function
            (apply [_ v]
              (let [m (get (:edges ast) v)
                    keys (keys m)
                    vals (map #(reify
                                 Action
                                 Object
                                 (equals [this o] (= (.toString this) (.toString o)))
                                 (hashCode [this] (.hashCode (.toString this)))
                                 (toString [_] (str %)))
                              keys)]
                (if (nil? keys)
                  {}
                  (clojure.set/rename-keys m (zipmap keys vals))))))
          expandRecursively)

    ;; Discourje
    :else
    (LTS. ast
          (reify
            Function
            (apply [_ ast] (successors ast)))
          expandRecursively)))

(defn bisimilar? [lts1 lts2]
  (LTSs/bisimilar lts1 lts2))