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

    ;; Vector
    (vector? ast)
    (mapv #(substitute % smap) ast)

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

    ;; Application
    (seq? ast)
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

    ;; Vector
    (vector? ast)
    (mapv #(unfold loop %) ast)

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

    ;; Application
    (seq? ast)
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

    ;; Vector
    (vector? ast)
    (every? terminated? ast)

    ;; If
    (= (:type ast) :if)
    (terminated? (if (eval (:condition ast)) (:branch1 ast) (:branch2 ast)))

    ;; Loop
    (= (:type ast) :loop)
    (terminated? (unfold ast (substitute (:body ast) (smap ast))))

    ;; Recur
    (= (:type ast) :recur)
    (throw (Exception.))

    ;; Application
    (seq? ast)
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

(defn eval-predicate [predicate]
  {:pre [(ast/predicate? predicate)]}
  (let [x (eval (:expr predicate))]
    (cond
      (class? x) #(= (type %) x)
      (fn? x) x)))

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
          predicate (eval-predicate (:predicate ast))]
      (cond (= (:type ast) :send)
            {(reify
               Send
               (getSender [_] (.toString sender))
               (getReceiver [_] (.toString receiver))
               (getPredicate [_] (reify Predicate (test [_ message] (predicate message))))
               Object
               (equals [this o] (= (.toString this) (.toString o)))
               (hashCode [this] (.hashCode (.toString this)))
               (toString [_] (str "!(" (:expr (:predicate ast)) "," sender "," receiver ")")))
             [(ast/end)]}
            (= (:type ast) :receive)
            {(reify
               Receive
               (getSender [_] (.toString sender))
               (getReceiver [_] (.toString receiver))
               (getPredicate [_] (reify Predicate (test [_ message] (predicate message))))
               Object
               (equals [this o] (= (.toString this) (.toString o)))
               (hashCode [this] (.hashCode (.toString this)))
               (toString [_] (str "?(" (:expr (:predicate ast)) "," sender "," receiver ")")))
             [(ast/end)]}
            (= (:type ast) :close)
            {(reify
               Close
               (getSender [_] (.toString sender))
               (getReceiver [_] (.toString receiver))
               Object
               (equals [this o] (= (.toString this) (.toString o)))
               (hashCode [this] (.hashCode (.toString this)))
               (toString [_] (str "C(" sender "," receiver ")")))
             [(ast/end)]}))

    ;; Choice
    (= (:type ast) :choice)
    (reduce (partial merge-with into) (map successors (:branches ast)))

    ;; Parallel
    (= (:type ast) :parallel)
    (let [branches (:branches ast)]
      (loop [i 0
             result {}]
        (if (= i (count branches))
          result
          (let [branch (nth branches i)
                ;; f inserts an "evaluated" branch into "unevaluated" branches
                f (fn [branch'] (ast/parallel (reduce into
                                                      [(subvec branches 0 i)
                                                       [branch']
                                                       (subvec branches (inc i) (count branches))])))
                ;; mapv-f maps f over a vector of branches
                mapv-f (fn [branches'] (mapv #(f %) branches'))
                ;; map-mapv-f maps mapv-f over a map from actions to vectors of branches
                map-mapv-f (fn [m] (map (fn [[k v]] {k (mapv-f v)}) m))]
            (recur (inc i) (merge-with into
                                       result
                                       (merge {} (reduce merge (map-mapv-f (successors branch))))))))))

    ;; Vector
    (vector? ast)
    (if (empty? ast)
      {}
      (if (terminated? (first ast))
        (successors (vec (rest ast)))
        (let [m (successors (first ast))]
          (update-in m (keys m)
                     (fn [x] (mapv #(smash (into (if (terminated? %) [] [%]) (rest ast))) x))))))

    ;; If
    (= (:type ast) :if)
    (successors (if (eval (:condition ast)) (:branch1 ast) (:branch2 ast)))

    ;; Loop
    (= (:type ast) :loop)
    (successors (unfold ast (substitute (:body ast) (smap ast))))

    ;; Recur
    (= (:type ast) :recur)
    (throw (Exception.))

    ;; Application
    (seq? ast)
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
    (LTS. #{(:v0 ast)}
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
    (LTS. #{ast}
          (reify
            Function
            (apply [_ ast] (successors ast)))
          expandRecursively)))

(defn bisimilar? [lts1 lts2]
  (LTSs/bisimilar lts1 lts2))