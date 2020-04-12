(ns discourje.spec
  (:require [clojure.java.shell :refer [sh]]
            [discourje.spec.ast :as ast]
            [discourje.spec.lts :as lts]
            [discourje.core.async.impl.monitors :as monitors]))
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
  [name-expr & index-exprs]
  `(ast/role '~name-expr (vec '~index-exprs)))

;;;;
;;;; Actions
;;;;

(defmacro -->
  ([sender-expr receiver-expr]
   `(--> ~'Object ~sender-expr ~receiver-expr))
  ([predicate-expr sender-expr receiver-expr]
   `(ast/sync (ast/predicate '~predicate-expr)
              (ast/role '~sender-expr)
              (ast/role '~receiver-expr))))

(defmacro -->>
  ([sender-expr receiver-expr]
   `(-->> ~'Object ~sender-expr ~receiver-expr))
  ([predicate-expr sender-expr receiver-expr]
   [`(ast/send (ast/predicate '~predicate-expr)
               (ast/role '~sender-expr)
               (ast/role '~receiver-expr))
    `(ast/receive (ast/role '~sender-expr)
                  (ast/role '~receiver-expr))]))

(defmacro close
  [sender-expr receiver-expr]
  `(ast/close (ast/role '~sender-expr) (ast/role '~receiver-expr)))

;;;;
;;;; Discourje: Nullary operators
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
  (s/apply ::pipe [t r-name 0 n]))

;;;;
;;;; TODO: Move the following functions elsewhere
;;;;

(defn expandRecursively!
  ([lts]
   (lts/expandRecursively! lts))
  ([lts bound]
   (lts/expandRecursively! lts bound)))

(defn lts
  ([ast]
   (lts ast true))
  ([ast expandRecursively]
   (let [lts (lts/lts ast)]
     (if expandRecursively (expandRecursively! lts))
     lts)))

(defn bisimilar? [lts1 lts2]
  (lts/bisimilar? lts1 lts2))

(defn not-bisimilar? [lts1 lts2]
  (not (bisimilar? lts1 lts2)))

(defn println [lts]
  (clojure.core/println (.toString lts)))

(defn ltsgraph [lts mcrl2-root-dir tmp-file]
  (spit tmp-file (.toString lts))
  (future (clojure.java.shell/sh (str mcrl2-root-dir "/bin/ltsgraph") tmp-file)))

(defn monitor [spec]
  (monitors/monitor (lts/lts spec)))

;;;;
;;;; TODO: Everything below is part of monitoring and should be put elsewhere at some point
;;;;

;(defmacro add-infrastructure
;  "adds infrastructure to the mep (channels)"
;  ([message-exchange-pattern]
;   `(generate-infrastructure ~message-exchange-pattern))
;  ([message-exchange-pattern custom-channels]
;   `(generate-infrastructure ~message-exchange-pattern ~custom-channels)))
;
;(defmacro custom-time
;  "Evaluates expr and prints the time it took.  Returns the value of expr."
;  [_] ;[expr]
;  `(let [start# (. System (nanoTime))
;         ];ret# ~expr]
;     (str "Elapsed time: " (/ (double (- (. System (nanoTime)) start#)) 1000000.0) " msecs")))