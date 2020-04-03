(ns discourje.core.async.spec
  (:require [discourje.core.async.impl.ast :as ast]))

;;;;
;;;; Roles
;;;;

(defn role [name]
  (fn
    ([] (str name))
    ([i] (str name "[" i "]"))))

;;;;
;;;; Actions
;;;;

(defmacro -->>
  ([sender receiver]
   (let [predicate 'Object
         channel (ast/channel sender receiver)]
     [(ast/send predicate channel) (ast/receive predicate channel)]))
  ([predicate sender receiver]
   (let [;pr `(if (fn? ~predicate) ~predicate (fn [~'x] (= (type ~'x) ~predicate)))
         pr predicate
         ch (ast/channel sender receiver)]
     [(ast/send pr ch) (ast/receive pr ch)])))

(defmacro close
  [sender receiver]
  `(ast/close (ast/channel ~sender ~receiver)))

;;;;
;;;; Nullary operators
;;;;

(defmacro end
  []
  (ast/end))

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
   `(ast/if-then '~condition ~branch))
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
;;;; Registry operators
;;;;

(defmacro def
  [name vars body & more]
  `(ast/register! '~name '~vars [~body ~@more]))

(defmacro apply
  [name exprs]
  `(concat ['~name] '~exprs))

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