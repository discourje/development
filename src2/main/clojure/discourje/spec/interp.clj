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
  {:pre [(or (ast/role? role) (fn? role))]}
  (let [role (if (fn? role) (role) role)]
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
                 (:index-exprs role))))))

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
  (case (:type ast)

    ;; Actions
    :sync (w/postwalk-replace smap ast)
    :send (w/postwalk-replace smap ast)
    :receive (w/postwalk-replace smap ast)
    :close (w/postwalk-replace smap ast)

    ;; Unary operators
    :end ast

    ;; Multiary operators
    :cat (ast/cat (mapv #(substitute % smap) (:branches ast)))
    :alt (ast/alt (mapv #(substitute % smap) (:branches ast)))
    :par (ast/par (mapv #(substitute % smap) (:branches ast)))
    :every (ast/every (:ast-f ast)
                      (:vars ast)
                      (w/postwalk-replace smap (:exprs ast))
                      (substitute (:branch ast) (apply dissoc smap (:vars ast))))

    ;; "Special forms" operators
    :if (ast/if (w/postwalk-replace smap (:test-expr ast))
          (substitute (:then ast) smap)
          (substitute (:else ast) smap))
    :loop (ast/loop (:name ast)
                    (:vars ast)
                    (w/postwalk-replace smap (:exprs ast))
                    (substitute (:body ast) (apply dissoc smap (:vars ast))))
    :recur (ast/recur (:name ast)
                      (w/postwalk-replace smap (:exprs ast)))

    ;; Misc operators
    :graph ast

    ;; Sessions
    :session (ast/session (:name ast)
                          (w/postwalk-replace smap (:exprs ast)))

    (throw (Exception.))))

(defn unfold [ast ast-loop]
  (case (:type ast)

    ;; Actions
    :sync ast
    :send ast
    :receive ast
    :close ast

    ;; Nullary operators
    :end ast

    ;; Multiary operators
    :cat (ast/cat (mapv #(unfold % ast-loop) (:branches ast)))
    :alt (ast/alt (mapv #(unfold % ast-loop) (:branches ast)))
    :par (ast/par (mapv #(unfold % ast-loop) (:branches ast)))
    :every (ast/every (:ast-f ast)
                      (:vars ast)
                      (:exprs ast)
                      (unfold (:branch ast) ast-loop))

    ;; "Special forms" operators
    :if (ast/if (:test-expr ast)
          (unfold (:then ast) ast-loop)
          (unfold (:else ast) ast-loop))
    :loop (ast/loop (:name ast)
                    (:vars ast)
                    (:exprs ast)
                    (if (= (:name ast-loop) (:name ast))
                      (:body ast)
                      (unfold (:body ast) ast-loop)))
    :recur (if (= (:name ast-loop) (:name ast))
             (ast/loop (:name ast-loop) (:vars ast-loop) (:exprs ast) (:body ast-loop))
             ast)

    ;; Misc operators
    :graph ast

    ;; Sessions
    :session ast

    (throw (Exception.))))

(defn eval-ast [ast]
  (case (:type ast)

    ;; Actions
    :sync (throw (Exception.))
    :send (throw (Exception.))
    :receive (throw (Exception.))
    :close (throw (Exception.))

    ;; Nullary operators
    :end (throw (Exception.))

    ;; Concatenation
    :cat (throw (Exception.))
    :alt (throw (Exception.))
    :par (throw (Exception.))
    :every (let [smaps (loop [vars (:vars ast)
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

    ;; "Special forms" operators
    :if (if (eval (:test-expr ast)) (:then ast) (:else ast))
    :loop (let [smap (loop [vars (:vars ast)
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
    :recur (throw (Exception.))

    ;; Misc operators
    :graph (throw (Exception.))

    ;; Sessions
    :session (let [name (:name ast)
                   exprs (:exprs ast)
                   body (:body (ast/get-ast name (count exprs)))
                   vars (:vars (ast/get-ast name (count exprs)))]
               (substitute body (zipmap vars (map eval exprs))))

    (throw (Exception.))))

;(defn subjects [ast]
;  (case (:type ast)
;
;    ;; Actions
;    :sync #{(:sender ast) (:receiver ast)}
;    :send #{(:sender ast)}
;    :receive #{(:receiver ast)}
;    :close #{(:sender ast) (:receiver ast)}
;
;    ;; Unary operators
;    (= (:type ast) :end)
;    #{}
;
;    ;; Multiary operators
;    :cat (reduce into (map #(subjects %) (:branches ast)))
;    :alt (reduce into (map #(subjects %) (:branches ast)))
;    :par (reduce into (map #(subjects %) (:branches ast)))
;    :every (subjects (eval-ast ast))
;
;    ;; "Special forms" operators
;    :if (subjects (eval-ast ast))
;    :loop (subjects (eval-ast ast))
;    :recur (throw (Exception.))
;
;    ;; Misc operators
;    :graph (throw (Exception.))
;
;    ;; Sessions
;    :session (throw (Exception.))
;    (throw (Exception.))))

(defn terminated? [ast unfolded]
  (case (:type ast)

    ;; Actions
    :sync false
    :send false
    :receive false
    :close false

    ;; Nullary operators
    :end true

    ;; Multiary operators
    :cat (every? #(terminated? % unfolded) (:branches ast))
    :alt (not (not-any? #(terminated? % unfolded) (:branches ast)))
    :par (every? #(terminated? % unfolded) (:branches ast))
    :every (terminated? (eval-ast ast) unfolded)

    ;; "Special forms" operators
    :if (terminated? (eval-ast ast) unfolded)
    :loop (if (contains? unfolded ast)
            false
            (terminated? (eval-ast ast) (conj unfolded ast)))
    :recur (throw (Exception.))

    ;; Misc operators
    :graph (empty? (get (:edges ast) (:v ast)))

    ;; Sessions
    :session (terminated? (eval-ast ast) unfolded)

    (throw (Exception.))))

(defn successors
  ([ast]
   (successors ast #{}))
  ([ast unfolded]
   (case (:type ast)

     ;; Actions
     :sync {ast [(ast/end)]}
     :send {ast [(ast/end)]}
     :receive {ast [(ast/end)]}
     :close {ast [(ast/end)]}

     ;; Nullary operators
     :end {}

     ;; Multiary operators
     :cat (let [branches (:branches ast)]
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
     :alt (let [branches (:branches ast)]
            (reduce (partial merge-with into) (map #(successors % unfolded) branches)))
     :par (let [branches (:branches ast)]
            (loop [i 0
                   m {}]
              (if (= i (count branches))
                m
                (recur (inc i) (merge-with into m (successors ast i unfolded))))))
     ;:dot (let [branches ast]
     ;       (if (empty? branches)
     ;         {}
     ;         (merge-with into
     ;                     ;; Rule 1
     ;                     (successors ast 0 unfolded)
     ;                     ;; Rule 2
     ;                     (let [branch (first branches)
     ;                           branches-after (rest branches)
     ;                           roles (subjects branch)]
     ;                       (filter (fn [[ast-action _]] (empty? (clojure.set/intersection roles (subjects ast-action))))
     ;                               (case (count branches-after)
     ;                                 0 {}
     ;                                 1 (successors (first branches-after) unfolded)
     ;                                 (successors (ast/cat (vec branches-after)) unfolded)))))))
     :every (successors (eval-ast ast) unfolded)

     ;; "Special forms" operators
     :if (successors (eval-ast ast) unfolded)
     :loop (if (contains? unfolded ast)
             {}
             (successors (eval-ast ast) (conj unfolded ast)))
     :recur (throw (Exception.))

     ;; Misc operators
     :graph (let [m (get (:edges ast) (:v ast))
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

     ;; Sessions
     :session (successors (eval-ast ast) unfolded)

     (throw (Exception.))))
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