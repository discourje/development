(ns discourje.core.async.spec
  (:require [discourje.core.async.impl.ast :as ast]
            [discourje.core.async.impl.lts :as lts]))

;;;;
;;;; Discourje: Roles
;;;;

(defn defrole
  [k name]
  (ast/put-role-name! k name))

(defmacro role
  [name-expr & index-exprs]
  `(ast/role '~name-expr (vec '~index-exprs)))

;;;;
;;;; Discourje: Actions
;;;;

(defmacro predicate
  [expr]
  `(ast/predicate '~expr))

(defmacro -->>
  ([sender-expr receiver-expr]
   `(-->> ~'Object ~sender-expr ~receiver-expr))
  ([predicate-expr sender-expr receiver-expr]
   [`(ast/send (ast/predicate '~predicate-expr)
               (ast/role '~sender-expr)
               (ast/role '~receiver-expr))
    `(ast/receive (ast/predicate '~predicate-expr)
                  (ast/role '~sender-expr)
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

;;;;
;;;; Discourje: Multiary operators
;;;;

(defmacro choice
  [branch & more]
  `(ast/choice [~branch ~@more]))

(defmacro parallel
  [branch & more]
  `(ast/parallel [~branch ~@more]))

;;;;
;;;; Discourje: Conditional operators
;;;;

(defmacro if
  ([condition branch]
   `(ast/if-then '~condition ~branch))
  ([condition branch1 branch2]
   `(ast/if-then-else '~condition ~branch1 ~branch2)))

;;;;
;;;; Discourje: Recursion operators
;;;;

(defmacro loop
  [name bindings body & more]
  `(ast/loop '~name '~bindings [~body ~@more]))

(defmacro recur
  [name & more]
  `(ast/recur '~name '[~@more]))

;;;;
;;;; Discourje: Registry operators
;;;;

(defmacro def
  [name vars body & more]
  `(ast/register! '~name '~vars [~body ~@more]))

(defmacro apply
  [name exprs]
  `(concat ['~name] '~exprs))

;;;;
;;;; Discourje: Patterns
;;;;

(require '[discourje.core.async.spec :as s])

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
;;;; Aldebaran
;;;;

(defmacro aldebaran [_ header & more]
  `(ast/graph (first '~header) '~more))

;;;;
;;;; LTS tools
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

;;;;
;;;; Temp
;;;;

(ltsgraph (lts (aldebaran des (0, 6, 4)
                          (0, "!(Object,alice,bob[0])", 1)
                          (0, "!(Object,alice,bob[1])", 2)
                          (0, "!(Object,alice,bob[2])", 3)
                          (1, "?(Object,alice,bob[0])", 0)
                          (2, "?(Object,alice,bob[1])", 0)
                          (3, "?(Object,alice,bob[2])", 0)))
          "/Applications/mCRL2.app/Contents"
          "/Users/sungshik/Desktop/lts.aut")

;;;;
;;;; TODO: Everything below is part of monitoring and should be put elsewhere at some point
;;;;

;(defmacro close!
;  "Close channel pair"
;  ([sender receiver infrastructure]
;   `(close-channel! ~sender ~receiver ~infrastructure))
;  ([channel]
;   `(close-channel! ~channel)))
;
;(defmacro closed?
;  "Check whether a channel is closed"
;  ([sender receiver infra]
;   `(channel-closed? ~sender ~receiver ~infra))
;  ([channel]
;   `(channel-closed? ~channel)))

;(defmacro add-infrastructure
;  "adds infrastructure to the mep (channels)"
;  ([message-exchange-pattern]
;   `(generate-infrastructure ~message-exchange-pattern))
;  ([message-exchange-pattern custom-channels]
;   `(generate-infrastructure ~message-exchange-pattern ~custom-channels)))
;
;(defmacro thread
;  "Execute body on thread"
;  [& body]
;  ;; copy-pasted from clojure.core.async:
;  `(clojure.core.async/thread-call (^:once fn* [] ~@body)))
;
;(defmacro custom-time
;  "Evaluates expr and prints the time it took.  Returns the value of expr."
;  [_] ;[expr]
;  `(let [start# (. System (nanoTime))
;         ];ret# ~expr]
;     (str "Elapsed time: " (/ (double (- (. System (nanoTime)) start#)) 1000000.0) " msecs")))