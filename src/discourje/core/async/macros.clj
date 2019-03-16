;macros.clj
(in-ns 'discourje.core.async.async)

;[rec] create recursion macro
(defmacro rec
  "Generate recursion"
  [name interaction & more]
  `(make-recursion name (vector interaction more)))
;[continue] create continue recursion macro
(defmacro continue
  "Continue recursion, matched by name"
  [name]
  `(do-recur name))
;[choice] create choice macro
;(defmacro choice
;  "Generate choice"
;  [branch & more]
;  `(make-choice (vector branch more)))
;[mep] specify mep macro
(defmacro mep [interactions]
  `(create-protocol (vector interactions)))