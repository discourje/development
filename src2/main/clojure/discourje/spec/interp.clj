(ns discourje.spec.interp
  (:require [clojure.walk :as w]
            [clojure.set :refer [rename-keys]]
            [discourje.spec.ast :as ast]))

(defn eval-predicate [predicate]
  {:pre [(ast/predicate? predicate)]}
  (let [x (eval (:expr predicate))]
    (cond
      (class? x) #(instance? x %)
      (fn? x) x)))

(defn eval-role [role]
  {:pre [(ast/role? role)]}
  (str (cond
         (string? (:name-expr role)) (:name-expr role)
         (keyword? (:name-expr role)) (ast/get-role-name (:name-expr role))
         :else (throw (Exception.)))
       (if (empty? (:index-exprs role))
         ""
         (mapv eval (:index-exprs role)))))

(defn substitute [ast smap]
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
    (terminated? (unfold ast (substitute (:body ast)
                                         (zipmap (:vars ast) (:exprs ast)))))

    ;; Recur
    (= (:type ast) :recur)
    (throw (Exception.))

    ;; Application
    (seq? ast)
    (let [name (first ast)
          exprs (rest ast)
          body (:body (get (get @ast/registry name) (count exprs)))
          vars (:vars (get (get @ast/registry name) (count exprs)))]
      (terminated? (substitute body (zipmap vars exprs))))

    :else (throw (Exception.))))

(defn successors [ast f-eval-action]
  (cond

    ;; End
    (= (:type ast) :end)
    {}

    ;; Action
    (contains? ast/action-types (:type ast))
    (f-eval-action ast)

    ;; Choice
    (= (:type ast) :choice)
    (reduce (partial merge-with into) (map #(successors % f-eval-action) (:branches ast)))

    ;; Parallel
    (= (:type ast) :parallel)
    (let [branches (:branches ast)]
      (loop [i 0
             result {}]
        (if (= i (count branches))
          result
          (let [branch (nth branches i)
                branches-before (subvec branches 0 i)
                branches-after (subvec branches (inc i) (count branches))
                ;; f inserts an "evaluated" branch between (possibly before or after) "unevaluated" branches
                f (fn [branch'] (ast/parallel (reduce into [branches-before
                                                            (if (and (terminated? branch')
                                                                     (empty? (successors branch' f-eval-action)))
                                                              []
                                                              [branch'])
                                                            branches-after])))
                ;; mapv-f maps f over a vector of branches
                mapv-f (fn [branches'] (mapv #(f %) branches'))
                ;; map-mapv-f maps mapv-f over a map from actions to vectors of branches
                map-mapv-f (fn [m] (map (fn [[k v]] {k (mapv-f v)}) m))]
            (recur (inc i) (merge-with into
                                       result
                                       (merge {} (reduce merge (map-mapv-f (successors branch f-eval-action))))))))))

    ;; Vector
    (vector? ast)
    (if (empty? ast)
      {}
      (merge-with into
                  (let [branch (first ast)
                        branches-after (vec (rest ast))
                        ;; f inserts an "evaluated" branch before "unevaluated" branches
                        f (fn [branch']
                            (let [branches' (reduce into [(if (and (terminated? branch')
                                                                   (empty? (successors branch' f-eval-action)))
                                                            []
                                                            [branch'])
                                                          branches-after])]
                              (if (= 1 (count branches'))
                                (first branches')
                                branches')))
                        ;; mapv-f maps f over a vector of branches
                        mapv-f (fn [branches'] (mapv #(f %) branches'))
                        ;; map-mapv-f maps mapv-f over a map from actions to vectors of branches
                        map-mapv-f (fn [m] (map (fn [[k v]] {k (mapv-f v)}) m))]
                    (merge {} (reduce merge (map-mapv-f (successors branch f-eval-action)))))
                  (if (terminated? (first ast))
                    (successors (vec (rest ast)) f-eval-action)
                    {})))

    ;; If
    (= (:type ast) :if)
    (successors (if (eval (:condition ast)) (:branch1 ast) (:branch2 ast)) f-eval-action)

    ;; Loop
    (= (:type ast) :loop)
    (successors (unfold ast (substitute (:body ast)
                                        (zipmap (:vars ast) (:exprs ast))))
                f-eval-action)

    ;; Recur
    (= (:type ast) :recur)
    (throw (Exception.))

    ;; Application
    (seq? ast)
    (let [name (first ast)
          exprs (rest ast)
          body (:body (get (get @ast/registry name) (count exprs)))
          vars (:vars (get (get @ast/registry name) (count exprs)))]
      (successors (substitute body (zipmap vars exprs)) f-eval-action))

    :else (throw (Exception.))))