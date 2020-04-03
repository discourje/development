(ns discourje.core.async.spec
  (:require [discourje.core.async.impl.dsl.syntax :as s]))

(defmacro -->>
  ([sender receiver]
   (let [predicate 'Object
         channel (s/channel sender receiver)]
     [(s/send predicate channel) (s/receive predicate channel)]))
  ([predicate sender receiver]
   (let [;pr `(if (fn? ~predicate) ~predicate (fn [~'x] (= (type ~'x) ~predicate)))
         pr predicate
         ch (s/channel sender receiver)]
     [(s/send pr ch) (s/receive pr ch)])))

(defmacro close
  [sender receiver]
  `(s/close (s/channel ~sender ~receiver)))

(defmacro choice
  [branch & more]
  `(s/choice [~branch ~@more]))

(defmacro parallel
  [branch & more]
  `(s/parallel [~branch ~@more]))

(defmacro if
  ([condition branch]
   `(s/if-then '~condition ~branch))
  ([condition branch1 branch2]
   `(s/if-then-else '~condition ~branch1 ~branch2)))

(defmacro loop
  [name bindings body & more]
  `(s/loop '~name '~bindings [~body ~@more]))

(defmacro recur
  [name & more]
  `(s/recur '~name '[~@more]))

(defmacro def
  [name vars body & more]
  `(s/register! '~name '~vars [~body ~@more]))

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