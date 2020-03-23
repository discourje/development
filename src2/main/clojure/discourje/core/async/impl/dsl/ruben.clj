(ns discourje.core.async.impl.dsl.ruben
  (:require [discourje.core.async.impl.dsl.syntax :refer :all])
  (:import (java.util UUID)))

(defn uuid []
  (.toString (UUID/randomUUID)))

(defmacro -->>
  "Create an Atomic-interaction"
  ([sender receiver]
   `(->interaction (uuid) (fn [~'x] true) ~sender ~receiver #{} nil))
  ([action sender receiver]
   `(if (fn? ~action)
      (->interaction (uuid) ~action ~sender ~receiver #{} nil)
      (->interaction (uuid) (fn [~'x] (= (type ~'x) ~action)) ~sender ~receiver #{} nil))))

(defmacro close
  "Create an close construct"
  [sender receiver]
  `(->closer (uuid) ~sender ~receiver nil))

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

(defmacro rec
  "Generate recursion"
  [name interaction & more]
  `(->recursion (uuid) ~name [~interaction ~@more] nil))

(defmacro continue
  "Continue recursion, matched by name"
  [name]
  `(->recur-identifier (uuid) ~name :recur nil))

(defmacro choice
  "Generate choice"
  [branch & more]
  `(->branch (uuid) [~branch ~@more] nil))

(defmacro parallel
  "Generate parallel"
  [parallels & more]
  `(->lateral (uuid) [~parallels ~@more] nil))

(defmacro mep
  "Generate message exchange pattern aka protocol"
  [interactions & more]
  `(->protocol [~interactions ~@more]))

;(defmacro add-infrastructure
;  "adds infrastructure to the mep (channels)"
;  ([message-exchange-pattern]
;   `(generate-infrastructure ~message-exchange-pattern))
;  ([message-exchange-pattern custom-channels]
;   `(generate-infrastructure ~message-exchange-pattern ~custom-channels)))

(defmacro thread
  "Execute body on thread"
  [& body]
  ;; copy-pasted from clojure.core.async:
  `(clojure.core.async/thread-call (^:once fn* [] ~@body)))

(defmacro custom-time
  "Evaluates expr and prints the time it took.  Returns the value of expr."
  [_] ;[expr]
  `(let [start# (. System (nanoTime))
         ];ret# ~expr]
     (str "Elapsed time: " (/ (double (- (. System (nanoTime)) start#)) 1000000.0) " msecs")))