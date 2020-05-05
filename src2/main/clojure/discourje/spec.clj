(ns discourje.spec
  (:gen-class)
  (:refer-clojure :exclude [cat loop * + apply])
  (:require [clojure.walk :as w]
            [discourje.spec.ast :as ast]))

(defn smap [env]
  `(zipmap '~(keys env) [~@(keys env)]))

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
   `(ast/role (w/postwalk-replace ~(smap &env) '~name-expr)
              (w/postwalk-replace ~(smap &env) '~index-exprs))))

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
     `(ast/cat [(ast/send ~predicate
                          ~sender
                          ~receiver)
                (ast/receive ~sender
                             ~receiver)]))))

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
                         `(ast/alt [(ast/sync (ast/predicate '~'Object)
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
    `(ast/alt ~branches)))

;;;;
;;;; Multiary operators
;;;;

(defmacro cat
  [branch & more]
  `(ast/cat [~branch ~@more]))

(defmacro alt
  [branch & more]
  `(ast/alt [~branch ~@more]))

(defmacro par
  [branch & more]
  `(ast/par [~branch ~@more]))

(defmacro cat-every
  [bindings branch]
  `(ast/every ast/cat
              (w/postwalk-replace ~(smap &env) '~bindings)
              ~branch))

(defmacro alt-every
  [bindings branch]
  `(ast/every ast/alt
              (w/postwalk-replace ~(smap &env) '~bindings)
              ~branch))

(defmacro par-every
  [bindings branch]
  `(ast/every ast/par
              (w/postwalk-replace ~(smap &env) '~bindings)
              ~branch))

;;;;
;;;; Conditional operators
;;;;

(defmacro if
  ([condition branch]
   `(ast/if-then-else (w/postwalk-replace ~(smap &env) '~condition)
                      ~branch
                      (ast/end)))
  ([condition branch1 branch2]
   `(ast/if-then-else (w/postwalk-replace ~(smap &env) '~condition)
                      ~branch1
                      ~branch2)))

;;;;
;;;; Recursion operators
;;;;

(defmacro let
  [bindings body & more]
  `(ast/loop nil
             (w/postwalk-replace ~(smap &env) '~bindings)
             (s/cat ~body ~@more)))

(defmacro loop
  [name bindings body & more]
  `(ast/loop (w/postwalk-replace ~(smap &env) '~name)
             (w/postwalk-replace ~(smap &env) '~bindings)
             (s/cat ~body ~@more)))

(defmacro recur
  [name & more]
  `(ast/recur (w/postwalk-replace ~(smap &env) '~name)
              '[~@more]))

;;;;
;;;; Regex operators
;;;;

(defonce ^:private ω-counter (atom 0))
(defonce ^:private *-counter (atom 0))
(defonce ^:private +-counter (atom 0))

(defmacro ω
  [body & more]
  (let [name (keyword (str "ω" (swap! ω-counter inc)))]
       `(ast/loop '~name
                  []
                  (ast/cat [~body
                            ~@more
                            (ast/recur '~name [])]))))

(defmacro omega
  [body & more]
  `(ω ~body ~@more))

(defmacro *
  [body & more]
  (let [name (keyword (str "*" (swap! *-counter inc)))]
       `(ast/loop '~name
                  []
                  (ast/alt [(ast/cat [~body
                                      ~@more
                                      (ast/recur '~name [])])
                            (ast/end)]))))

(defmacro +
  [body & more]
  (let [name (keyword (str "+" (swap! +-counter inc)))]
       `(ast/cat [~body
                  ~@more
                  (ast/loop '~name
                            []
                            (ast/alt [(ast/cat [~body
                                                ~@more
                                                (ast/recur '~name [])])
                                      (ast/end)]))])))
(defmacro ?
  [body & more]
  `(ast/alt [(ast/cat [~body ~@more])
             (ast/end)]))

;;;;
;;;; Definition operators
;;;;

(defmacro def
  [name vars body & more]
  `(ast/register! (w/postwalk-replace ~(smap &env) '~name)
                  (w/postwalk-replace ~(smap &env) '~vars)
                  (s/cat ~body ~@more)))

(defmacro apply
  [name exprs]
  `(concat [(w/postwalk-replace ~(smap &env) '~name)]
           (w/postwalk-replace ~(smap &env) '~exprs)))

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
            (s/cat (s/-->> t (r-name i) (r-name (inc i)))
                   (s/recur pipe (inc i))))))

(s/def ::pipe [t r-name n]
  (s/apply ::pipe ['t r-name 0 n]))
