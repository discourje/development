(ns discourje.spec
  (:require [discourje.spec.ast :as ast]))

;;;;
;;;; Predicates
;;;;

(defmacro predicate
  [expr]
  `(ast/predicate '~expr))

;;;;
;;;; Roles
;;;;

(defn defrole
  [k name]
  (ast/put-role-name! k name))

(defmacro role
  ([name-expr]
   `(role ~name-expr []))
  ([name-expr index-exprs]
   {:pre [(or (string? name-expr) (keyword? name-expr) (symbol? name-expr))
          (vector? index-exprs)]}
   `(ast/role '~name-expr '~index-exprs)))

;;;;
;;;; Actions
;;;;

(defn- predicate-form [predicate]
  (cond
    ;; predicate is of the form: (predicate expr)
    (and (seq? predicate) (= 'discourje.spec.ast/predicate (first (macroexpand predicate))))
    predicate

    ;; predicate is of the form: x
    (and (symbol? predicate) (not (resolve predicate)))
    `'~predicate

    ;; predicate is of the form: expr
    :else (list 'discourje.spec/predicate predicate)))

(defn- role-form [role]
  (cond
    ;; role is of the form: (role name-expr index-exprs)
    (and (seq? role) (= 'discourje.spec.ast/role (first (macroexpand role))))
    role

    ;; role is of the form: (name-expr & index-exprs)
    (seq? role)
    (list 'discourje.spec.ast/role `'~(first role) (mapv (fn [x] `'~x) (rest role)))

    ;; role is of the form: x
    (symbol? role)
    `'~role

    ;; role is of the form: "alice" or :alice
    :else (list 'discourje.spec.ast/role `'~role)))

(defmacro -->
  ([sender receiver]
   `(--> (predicate ~'Object) ~sender ~receiver))
  ([predicate sender receiver]
   (let [predicate (predicate-form predicate)
         sender (role-form sender)
         receiver (role-form receiver)]
     `(ast/sync ~predicate
                ~sender
                ~receiver))))

(defmacro -->>
  ([sender receiver]
   `(-->> (predicate ~'Object) ~sender ~receiver))
  ([predicate sender receiver]
   (let [predicate (predicate-form predicate)
         sender (role-form sender)
         receiver (role-form receiver)]
     [`(ast/send ~predicate
                 ~sender
                 ~receiver)
      `(ast/receive ~sender
                    ~receiver)])))

(defmacro close
  [sender-expr receiver-expr]
  `(ast/close (ast/role '~sender-expr) (ast/role '~receiver-expr)))

;;;;
;;;; Nullary operators
;;;;

(defmacro end
  []
  (ast/end))

(defmacro any
  [role-exprs]
  (let [branches (mapv (fn [[sender-expr receiver-expr]]
                         `(ast/choice [(ast/sync (ast/predicate '~'Object)
                                                 (ast/role '~sender-expr)
                                                 (ast/role '~receiver-expr))
                                       (ast/send (ast/predicate '~'Object)
                                                 (ast/role '~sender-expr)
                                                 (ast/role '~receiver-expr))
                                       (ast/receive (ast/role '~sender-expr)
                                                    (ast/role '~receiver-expr))
                                       (ast/close (ast/role '~sender-expr)
                                                  (ast/role '~receiver-expr))]))
                       (for [sender role-exprs
                             receiver role-exprs
                             :when (not= sender receiver)]
                         [sender receiver]))]
    `(ast/choice ~branches)))

;;;;
;;;; Multiary operators
;;;;

(defmacro choice
  [branch & more]
  `(ast/choice [~branch ~@more]))

(defmacro parallel
  [branch & more]
  `(ast/parallel [~branch ~@more]))

;;;;
;;;; Conditional operators
;;;;

(defmacro if
  ([condition branch]
   `(ast/if-then-else '~condition ~branch (ast/end)))
  ([condition branch1 branch2]
   `(ast/if-then-else '~condition ~branch1 ~branch2)))

;;;;
;;;; Recursion operators
;;;;

(defmacro loop
  [name bindings body & more]
  `(ast/loop '~name '~bindings [~body ~@more]))

(defmacro recur
  [name & more]
  `(ast/recur '~name '[~@more]))

;;;;
;;;; Regex operators
;;;;

(def ^:private *-counter (atom 0))

(defmacro *
  [body & more]
  (let [name (keyword (str "*" (swap! *-counter inc)))]
    `(ast/loop '~name [] (ast/choice [[~body ~@more (ast/recur '~name [])]
                                      (ast/end)]))))

;;;;
;;;; Definition operators
;;;;

(defmacro def
  [name vars body & more]
  `(ast/register! '~name '~vars [~body ~@more]))

(defmacro apply
  [name exprs]
  `(concat ['~name] '~exprs))

;;;;
;;;; Aldebaran
;;;;

(defmacro aldebaran [_ header & more]
  `(ast/aldebaran (first '~header) '~more))

;;;;
;;;; Patterns
;;;;

(require '[discourje.spec :as s])

(s/def ::-->>not [t r1 r2]
  (s/-->> (fn [x] (not= (type x) t)) r1 r2))

(s/def ::pipe [t r-name min max]
  (s/loop pipe [i min]
          (s/if (< i (dec max))
            [(s/-->> t (r-name i) (r-name (inc i)))
             (s/recur pipe (inc i))])))

(s/def ::pipe [t r-name n]
  (s/apply ::pipe ['t r-name 0 n]))
