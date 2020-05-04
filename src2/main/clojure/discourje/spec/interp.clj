(ns discourje.spec.interp
  (:require [clojure.walk :as w]
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
         :else (throw (Exception. (str (type (:name-expr role))))))
       (if (empty? (:index-exprs role))
         ""
         (mapv #(let [index (eval %)]
                  (if (number? index)
                    index
                    (throw (Exception.))))
               (:index-exprs role)))))

(defn eval-action [action f]
  {:pre [(ast/action? action)]}
  (let [type (:type action)
        predicate (eval-predicate (:predicate action))
        sender (eval-role (:sender action))
        receiver (eval-role (:receiver action))]
    (f (str (case (:type action)
              :sync "‽"
              :send "!"
              :receive "?"
              :close "C"
              (throw (Exception.))) "("
            (if (contains? #{:sync :send} (:type action)) (str (:expr (:predicate action)) ",") "")
            sender ","
            receiver ")")
       type
       predicate
       sender
       receiver)))

(defn substitute [ast smap]
  (cond

    ;; End
    (= (:type ast) :end)
    ast

    ;; Action
    (ast/action? ast)
    (w/postwalk-replace smap ast)

    ;; Concatenation
    (= (:type ast) :cat)
    (ast/cat (mapv #(substitute % smap) (:branches ast)))

    ;; Alternatives
    (= (:type ast) :alt)
    (ast/alt (mapv #(substitute % smap) (:branches ast)))

    ;; Parallel
    (= (:type ast) :par)
    (ast/par (mapv #(substitute % smap) (:branches ast)))

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

    ;; Aldebaran
    (= (:type ast) :aldebaran)
    ast

    :else (throw (Exception.))))

(defn unfold [loop ast]
  (cond

    ;; End
    (= (:type ast) :end)
    ast

    ;; Action
    (ast/action? ast)
    ast

    ;; Concatenation
    (= (:type ast) :cat)
    (ast/cat (mapv #(unfold loop %) (:branches ast)))

    ;; Alternatives
    (= (:type ast) :alt)
    (ast/alt (mapv #(unfold loop %) (:branches ast)))

    ;; Parallel
    (= (:type ast) :par)
    (ast/par (mapv #(unfold loop %) (:branches ast)))

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

    ;; Aldebaran
    (= (:type ast) :aldebaran)
    ast

    :else (throw (Exception.))))

(defn terminated? [ast unfolded]
  (cond

    ;; End
    (= (:type ast) :end)
    true

    ;; Action
    (ast/action? ast)
    false

    ;; Concatenation
    (= (:type ast) :cat)
    (every? #(terminated? % unfolded) (:branches ast))

    ;; Alternatives
    (= (:type ast) :alt)
    (not (not-any? #(terminated? % unfolded) (:branches ast)))

    ;; Parallel
    (= (:type ast) :par)
    (every? #(terminated? % unfolded) (:branches ast))

    ;; Vector
    (vector? ast)
    (every? #(terminated? % unfolded) ast)

    ;; If
    (= (:type ast) :if)
    (terminated? (if (eval (:condition ast)) (:branch1 ast) (:branch2 ast)) unfolded)

    ;; Loop
    (= (:type ast) :loop)
    (if (contains? unfolded ast)
      false
      (terminated? (unfold ast (substitute (:body ast)
                                           (zipmap (:vars ast) (map eval (:exprs ast)))))
                   (conj unfolded ast)))

    ;; Recur
    (= (:type ast) :recur)
    (throw (Exception.))

    ;; Application
    (seq? ast)
    (let [name (first ast)
          exprs (rest ast)
          body (:body (get (get @ast/registry name) (count exprs)))
          vars (:vars (get (get @ast/registry name) (count exprs)))]
      (terminated? (substitute body (zipmap vars (map eval exprs))) unfolded))

    ;; Aldebaran
    (= (:type ast) :aldebaran)
    (empty? (get (:edges ast) (:v ast)))

    :else (throw (Exception.))))

(defn successors
  ([ast f-action]
   (successors ast #{} f-action))
  ([ast unfolded f-action]
   (cond

     ;; End
     (= (:type ast) :end)
     {}

     ;; Action
     (ast/action? ast)
     {(eval-action ast f-action) [(ast/end)]}

     ;; Concatenation
     (= (:type ast) :cat)
     (let [branches (:branches ast)]
       (if (empty? branches)
         {}
         (merge-with into

                     ;; Rule 1
                     (let [branch (first branches)
                           branches-after (rest branches)
                           ;; f inserts an "evaluated" branch before "unevaluated" branches
                           f (fn [branch']
                               (ast/cat (reduce into [[] [branch'] branches-after])))
                           ;; mapv-f maps f over a vector of branches
                           mapv-f (fn [branches'] (mapv #(f %) branches'))
                           ;; map-mapv-f maps mapv-f over a map from actions to vectors of branches
                           map-mapv-f (fn [m] (map (fn [[k v]] {k (mapv-f v)}) m))]
                       (merge {} (reduce merge (map-mapv-f (successors branch unfolded f-action)))))

                     ;; Rule 2
                     (let [branch (first branches)
                           branches-after (rest branches)]
                       (if (terminated? branch #{})
                         (case (count branches-after)
                           0 {}
                           1 (successors (first branches-after) unfolded f-action)
                           (successors (ast/cat (vec branches-after)) unfolded f-action))
                         {})))))

     ;; Alternatives
     (= (:type ast) :alt)
     (reduce (partial merge-with into) (map #(successors % unfolded f-action) (:branches ast)))

     ;; Parallel
     (= (:type ast) :par)
     (let [branches (:branches ast)]
       (loop [i 0
              result {}]
         (if (= i (count branches))
           result
           (let [branch (nth branches i)
                 branches-before (subvec branches 0 i)
                 branches-after (subvec branches (inc i) (count branches))
                 ;; f inserts an "evaluated" branch between (possibly before or after) "unevaluated" branches
                 f (fn [branch']
                     (ast/par (reduce into [[] branches-before [branch'] branches-after])))
                 ;; mapv-f maps f over a vector of branches
                 mapv-f (fn [branches'] (mapv #(f %) branches'))
                 ;; map-mapv-f maps mapv-f over a map from actions to vectors of branches
                 map-mapv-f (fn [m] (map (fn [[k v]] {k (mapv-f v)}) m))]
             (recur (inc i) (merge-with into
                                        result
                                        (merge {} (reduce merge (map-mapv-f (successors branch unfolded f-action))))))))))

     ;; Every
     (= (:type ast) :every)
     (let [smaps (loop [vars (:vars ast)
                        exprs (:exprs ast)
                        smaps [{}]]
                   (if (empty? vars)
                     smaps
                     (let [var (first vars)
                           expr (first exprs)
                           f (fn [smap] (mapv (fn [val]
                                                (assoc smap var val))
                                              (eval (w/postwalk-replace smap expr))))]
                       (recur (rest vars)
                              (rest exprs)
                              (reduce into (mapv f smaps))))))
           branches (mapv #(substitute (:branch ast) %) smaps)]
       (successors ((:ast-f ast) branches) unfolded f-action))

     ;; Vector
     (vector? ast)
     (if (empty? ast)
       {}
       (merge-with into
                   (let [branch (first ast)
                         branches-after (vec (rest ast))
                         ;; f inserts an "evaluated" branch before "unevaluated" branches
                         f (fn [branch']
                             (let [branches' (reduce into [(if (and (terminated? branch' #{})
                                                                    (empty? (successors branch' unfolded f-action)))
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
                     (merge {} (reduce merge (map-mapv-f (successors branch unfolded f-action)))))
                   (if (terminated? (first ast) #{})
                     (successors (vec (rest ast)) unfolded f-action)
                     {})))

     ;; If
     (= (:type ast) :if)
     (successors (if (eval (:condition ast)) (:branch1 ast) (:branch2 ast)) unfolded f-action)

     ;; Loop
     (= (:type ast) :loop)
     (if (contains? unfolded ast)
       {}
       (successors (unfold ast (substitute (:body ast)
                                           (loop [vars (:vars ast)
                                                  exprs (:exprs ast)
                                                  smap {}]
                                             (if (empty? vars)
                                               smap
                                               (let [var (first vars)
                                                     expr (first exprs)
                                                     val (eval (w/postwalk-replace smap expr))]
                                                 (recur (rest vars)
                                                        (rest exprs)
                                                        (assoc smap var val)))))))
                   (conj unfolded ast)
                   f-action))

     ;; Recur
     (= (:type ast) :recur)
     (throw (Exception.))

     ;; Application
     (seq? ast)
     (let [name (eval (first ast))
           exprs (rest ast)
           body (:body (get (get @ast/registry name) (count exprs)))
           vars (:vars (get (get @ast/registry name) (count exprs)))]
       (successors (substitute body (zipmap vars (map eval exprs))) unfolded f-action))

     ;; Aldebaran
     (= (:type ast) :aldebaran)
     (let [m (get (:edges ast) (:v ast))
           keys (map #(eval-action % f-action) (keys m))
           vals (map (fn [k]
                       (let [vertices (get m k)]
                         (if (empty? vertices)
                           [(ast/end)]
                           (mapv #(assoc ast :v %) vertices))))
                     (clojure.core/keys m))]
       (if (nil? keys)
         {}
         (zipmap keys vals)))

     :else (throw (Exception.)))))
