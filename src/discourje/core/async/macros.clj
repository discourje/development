;macros.clj
(in-ns 'discourje.core.async.async)

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
  [interactions]
  `(->protocol [~interactions]))
