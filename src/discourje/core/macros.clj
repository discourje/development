;macros.clj
(in-ns 'discourje.core.async)

(defmacro -->>
  "Create an Atomic-interaction"
  [action sender receiver]
    `(->interaction (uuid/v1) ~action ~sender ~receiver nil))

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

(defmacro chan
  "create a custom channel"
  [sender receiver buffer]
  `(if (nil? ~buffer)
     (->channel ~sender ~receiver (clojure.core.async/chan) nil nil)
     (->channel ~sender ~receiver (clojure.core.async/chan ~buffer) ~buffer nil)))

(defmacro msg
  "Generate a message"
  [label content]
  `(->message ~label ~content))

(defmacro thread
  "Execute body on thread"
  [body]
  `(async/thread ~body))

(defmacro close!
  "Close the channel"
  [channel]
  `(async/close! (get-chan ~channel)))

(defmacro close-infrastructure!
  "Close all channels of the Discourje infrastructure"
  [infra]
  `(doseq [c ~infra] (close! c)))

(defmacro custom-time
  "Evaluates expr and prints the time it took.  Returns the value of expr."
  [expr]
  `(let [start# (. System (nanoTime))
         ret# ~expr]
     (str "Elapsed time: " (/ (double (- (. System (nanoTime)) start#)) 1000000.0) " msecs")))