;macros.clj
(in-ns 'discourje.core.async)

(defmacro -->>
  "Create an Atomic-interaction"
  [action sender receiver]
  `(->interaction (uuid/v1) ~action ~sender ~receiver #{} nil))

(defmacro close
  "Create an close construct"
  [sender receiver]
  `(->closer (uuid/v1) ~sender ~receiver nil))

(defmacro close!
  "Close channel pair"
  ([sender receiver infrastructure]
   `(close-channel! ~sender ~receiver ~infrastructure))
  ([channel]
   `(close-channel! ~channel)))

(defmacro closed?
  "Check whether a channel is closed"
  ([sender receiver infra]
   `(channel-closed? ~sender ~receiver ~infra))
  ([channel]
   `(channel-closed? ~channel)))

(defmacro rec
  "Generate recursion"
  [name interaction & more]
  `(->recursion (uuid/v1) ~name [~interaction ~@more] nil))

(defmacro continue
  "Continue recursion, matched by name"
  [name]
  `(->recur-identifier (uuid/v1) ~name :recur nil))

(defmacro choice
  "Generate choice"
  [branch & more]
  `(->branch (uuid/v1) [~branch ~@more] nil))

(defmacro parallel
  "Generate parallel"
  [parallels & more]
  `(->lateral (uuid/v1) [~parallels ~@more] nil))

(defmacro mep
  "Generate message exchange pattern aka protocol"
  [interactions & more]
  `(->protocol [~interactions ~@more]))

(defmacro add-infrastructure
  "adds infrastructure to the mep (channels)"
  ([message-exchange-pattern]
   `(generate-infrastructure ~message-exchange-pattern))
  ([message-exchange-pattern custom-channels]
   `(generate-infrastructure ~message-exchange-pattern ~custom-channels)))

(defmacro thread
  "Execute body on thread"
  [& body]
  ;; copy-pasted from clojure.core.async:
  `(clojure.core.async/thread-call (^:once fn* [] ~@body)))

(defmacro custom-time
  "Evaluates expr and prints the time it took.  Returns the value of expr."
  [expr]
  `(let [start# (. System (nanoTime))
         ret# ~expr]
     (str "Elapsed time: " (/ (double (- (. System (nanoTime)) start#)) 1000000.0) " msecs")))