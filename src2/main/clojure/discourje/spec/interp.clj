(ns discourje.spec.interp
  (:refer-clojure :exclude [eval])
  (:require [clojure.walk :as w]
            [discourje.spec.ast :as ast]))

(def ^:dynamic *hist* nil)

(defn eval [expr]
  (clojure.core/eval (if *hist*
                       (w/postwalk-replace {'&hist 'discourje.spec.interp/*hist*} expr)
                       expr)))

(defonce ^:private eval-predicate-cache (atom (hash-map)))

(defn eval-predicate [predicate]
  {:pre [(ast/predicate? predicate)]}
  (if-let [f (get @eval-predicate-cache predicate)]
    f
    (let [x (eval (:expr predicate))
          f (cond
              (fn? x) x
              (class? x) #(instance? x %)
              :else (fn [message] (= message x)))
          _ (swap! eval-predicate-cache #(assoc % predicate f))]
      f)))

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

(defrecord Action [name type predicate sender receiver])

(defn action? [x]
  (= (type x) Action))

(defn action
  ([ast-action]
   {:pre [(ast/action? ast-action)]}
   (let [type (:type ast-action)
         predicate (eval-predicate (:predicate ast-action))
         sender (eval-role (:sender ast-action))
         receiver (eval-role (:receiver ast-action))
         name (str (case (:type ast-action)
                     :sync "‽"
                     :send "!"
                     :receive "?"
                     :close "C"
                     (throw (Exception.))) "("
                   (if (contains? #{:sync :send} (:type ast-action)) (str (:expr (:predicate ast-action)) ",") "")
                   sender ","
                   receiver ")")]
     (action name type predicate sender receiver)))

  ([name type predicate sender receiver]
   {:pre [(string? name)
          (contains? #{:sync :send :receive :close} type)
          (fn? predicate)
          (string? sender)
          (string? receiver)]}
   (->Action name type predicate sender receiver)))

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

    ;; Every
    (= (:type ast) :every)
    (ast/every (:ast-f ast)
               (:vars ast)
               (w/postwalk-replace smap (:exprs ast))
               (substitute (:branch ast) (apply dissoc smap (:vars ast))))

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

(defn unfold [ast ast-loop]
  (cond

    ;; End
    (= (:type ast) :end)
    ast

    ;; Action
    (ast/action? ast)
    ast

    ;; Concatenation
    (= (:type ast) :cat)
    (ast/cat (mapv #(unfold % ast-loop) (:branches ast)))

    ;; Alternatives
    (= (:type ast) :alt)
    (ast/alt (mapv #(unfold % ast-loop) (:branches ast)))

    ;; Parallel
    (= (:type ast) :par)
    (ast/par (mapv #(unfold % ast-loop) (:branches ast)))

    ;; Every
    (= (:type ast) :every)
    (ast/every (:ast-f ast) (:vars ast) (:exprs ast) (unfold (:branch ast) ast-loop))

    ;; Vector
    (vector? ast)
    (mapv #(unfold % ast-loop) ast)

    ;; If
    (= (:type ast) :if)
    (ast/if-then-else (:condition ast)
                      (unfold (:branch1 ast) ast-loop)
                      (unfold (:branch2 ast) ast-loop))

    ;; Loop
    (= (:type ast) :loop)
    (ast/loop (:name ast)
              (:vars ast)
              (:exprs ast)
              (if (= (:name ast-loop) (:name ast)) (:body ast) (unfold (:body ast) ast-loop)))

    ;; Recur
    (= (:type ast) :recur)
    (if (= (:name ast-loop) (:name ast))
      (ast/loop (:name ast-loop) (:vars ast-loop) (:exprs ast) (:body ast-loop))
      ast)

    ;; Application
    (seq? ast)
    ast

    ;; Aldebaran
    (= (:type ast) :aldebaran)
    ast

    :else (throw (Exception.))))

(defn eval-ast [ast]
  (cond

    ;; End
    (= (:type ast) :end)
    (throw (Exception.))

    ;; Action
    (ast/action? ast)
    (throw (Exception.))

    ;; Concatenation
    (= (:type ast) :cat)
    (throw (Exception.))

    ;; Alternatives
    (= (:type ast) :alt)
    (throw (Exception.))

    ;; Parallel
    (= (:type ast) :par)
    (throw (Exception.))

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
      ((:ast-f ast) branches))

    ;; Vector
    (vector? ast)
    (throw (Exception.))

    ;; If
    (= (:type ast) :if)
    (if (eval (:condition ast)) (:branch1 ast) (:branch2 ast))

    ;; Loop
    (= (:type ast) :loop)
    (let [smap (loop [vars (:vars ast)
                      exprs (:exprs ast)
                      smap {}]
                 (if (empty? vars)
                   smap
                   (let [var (first vars)
                         expr (first exprs)
                         val (eval (w/postwalk-replace smap expr))]
                     (recur (rest vars)
                            (rest exprs)
                            (assoc smap var val)))))]
      (unfold (substitute (:body ast) smap) ast))

    ;; Recur
    (= (:type ast) :recur)
    (throw (Exception.))

    ;; Application
    (seq? ast)
    (let [name (eval (first ast))
          exprs (rest ast)
          body (:body (get (get @ast/registry name) (count exprs)))
          vars (:vars (get (get @ast/registry name) (count exprs)))]
      (substitute body (zipmap vars (map eval exprs))))

    ;; Aldebaran
    (= (:type ast) :aldebaran)
    (throw (Exception.))

    :else (throw (Exception.))))

(defn subjects [ast]
  (cond

    ;; End
    (= (:type ast) :end)
    #{}

    ;; Action
    (ast/action? ast)
    (case (:type ast)
      :sync #{(:sender ast) (:receiver ast)}
      :send #{(:sender ast)}
      :receive #{(:receiver ast)}
      :close #{(:sender ast) (:receiver ast)}
      (throw (Exception.)))

    ;; Concatenation
    (= (:type ast) :cat)
    (reduce into (map #(subjects %) (:branches ast)))

    ;; Alternatives
    (= (:type ast) :alt)
    (reduce into (map #(subjects %) (:branches ast)))

    ;; Parallel
    (= (:type ast) :par)
    (reduce into (map #(subjects %) (:branches ast)))

    ;; Every
    (= (:type ast) :every)
    (subjects (eval-ast ast))

    ;; Vector
    (vector? ast)
    (reduce into (map #(subjects %) ast))

    ;; If
    (= (:type ast) :if)
    (subjects (eval-ast ast))

    ;; Loop
    (= (:type ast) :loop)
    (subjects (eval-ast ast))

    ;; Recur
    (= (:type ast) :recur)
    (throw (Exception.))

    ;; Application
    (seq? ast)
    (throw (Exception.))

    ;; Aldebaran
    (= (:type ast) :aldebaran)
    (throw (Exception.))

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

    ;; Every
    (= (:type ast) :every)
    (terminated? (eval-ast ast) unfolded)

    ;; Vector
    (vector? ast)
    (every? #(terminated? % unfolded) ast)

    ;; If
    (= (:type ast) :if)
    (terminated? (eval-ast ast) unfolded)

    ;; Loop
    (= (:type ast) :loop)
    (if (contains? unfolded ast)
      false
      (terminated? (eval-ast ast) (conj unfolded ast)))

    ;; Recur
    (= (:type ast) :recur)
    (throw (Exception.))

    ;; Application
    (seq? ast)
    (terminated? (eval-ast ast) unfolded)

    ;; Aldebaran
    (= (:type ast) :aldebaran)
    (empty? (get (:edges ast) (:v ast)))

    :else (throw (Exception.))))

(defn successors
  ([ast]
   (successors ast #{}))
  ([ast unfolded]
   (cond

     ;; End
     (= (:type ast) :end)
     {}

     ;; Action
     (ast/action? ast)
     {ast [(ast/end)]}

     ;; Concatenation
     (= (:type ast) :cat)
     (let [branches (:branches ast)]
       (if (empty? branches)
         {}
         (merge-with into

                     ;; Rule 1
                     (successors ast 0 unfolded)

                     ;; Rule 2
                     (let [branch (first branches)
                           branches-after (rest branches)]
                       (if (terminated? branch #{})
                         (case (count branches-after)
                           0 {}
                           1 (successors (first branches-after) unfolded)
                           (successors (ast/cat (vec branches-after)) unfolded))
                         {})))))

     ;; Alternatives
     (= (:type ast) :alt)
     (let [branches (:branches ast)]
       (reduce (partial merge-with into) (map #(successors % unfolded) branches)))

     ;; Parallel
     (= (:type ast) :par)
     (let [branches (:branches ast)]
       (loop [i 0
              m {}]
         (if (= i (count branches))
           m
           (recur (inc i) (merge-with into m (successors ast i unfolded))))))

     ;; Every
     (= (:type ast) :every)
     (successors (eval-ast ast) unfolded)

     ;; Vector
     (vector? ast)
     (let [branches ast]
       (if (empty? branches)
         {}
         (merge-with into

                     ;; Rule 1
                     (successors ast 0 unfolded)

                     ;; Rule 2
                     (let [branch (first branches)
                           branches-after (rest branches)
                           roles (subjects branch)]
                       (filter (fn [[ast-action _]] (empty? (clojure.set/intersection roles (subjects ast-action))))
                               (case (count branches-after)
                                 0 {}
                                 1 (successors (first branches-after) unfolded)
                                 (successors (ast/cat (vec branches-after)) unfolded)))))))

     ;; If
     (= (:type ast) :if)
     (successors (eval-ast ast) unfolded)

     ;; Loop
     (= (:type ast) :loop)
     (if (contains? unfolded ast)
       {}
       (successors (eval-ast ast) (conj unfolded ast)))

     ;; Recur
     (= (:type ast) :recur)
     (throw (Exception.))

     ;; Application
     (seq? ast)
     (successors (eval-ast ast) unfolded)

     ;; Aldebaran
     (= (:type ast) :aldebaran)
     (let [m (get (:edges ast) (:v ast))
           ks (keys m)
           vals (map (fn [k]
                       (let [vertices (get m k)]
                         (if (empty? vertices)
                           [(ast/end)]
                           (mapv #(assoc ast :v %) vertices))))
                     (keys m))]
       (if (nil? (keys m))
         {}
         (zipmap ks vals)))

     :else (throw (Exception.))))

  ([ast-multiary i unfolded]
   (let [branches (:branches ast-multiary)
         ast-f (case (:type ast-multiary)
                 :cat ast/cat
                 :alt ast/alt
                 :par ast/par
                 (throw (Exception.)))
         ith (nth branches i)
         ith-before (subvec branches 0 i)
         ith-after (subvec branches (inc i) (count branches))
         m (successors ith unfolded)]
     (zipmap (keys m) (mapv #(mapv (fn [ith'] (ast-f (reduce into [ith-before [ith'] ith-after]))) %) (vals m))))))

(defn successors-with-hist
  [ast hist]
  (binding [*hist* hist]
    (loop [successors (successors ast #{})
           successors-with-hist {}]
      (if (empty? successors)
        successors-with-hist
        (let [[ast-action asts] (first successors)]
          (recur (rest successors)
                 (assoc successors-with-hist
                   ast-action
                   (mapv (fn [ast] [ast (conj hist ast-action)]) asts))))))))