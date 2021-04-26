(ns discourje.core.validation.causality-async
  (:require [discourje.core.spec :as s]))

(s/defrole ::a)
(s/defrole ::b)
(s/defrole ::c)
(s/defrole ::d)

;; All used messages have a clear cause
(s/defsession ::protocol-trivial-correct []
              [(s/-->> ::a ::b)
               (s/-->> ::b ::a)
               (s/close ::a ::b)
               (s/close ::b ::a)
               ])

;; message c a has no cause
(s/defsession ::protocol-trivial-incorrect []
              [
               (s/-->> ::a ::b)
               (s/-->> ::c ::a)
               (s/close ::a ::b)
               (s/close ::c ::a)
               ])

;; the s/par keyword will lead to paths where some messages have no direct cause, but they have an indirect cause:
;; (--> a b) (--> a c) (--> b c)
;; (--> a c) (--> a b) (--> b c)
(s/defsession ::protocol-non-trivial-correct []
              (s/par
                [(s/-->> ::a ::b)
                 (s/-->> ::b ::c)
                 ]
                (s/-->> ::a ::c))
              (s/close ::a ::b)
              (s/close ::a ::c)
              (s/close ::b ::c)
              )

;; (--> d a) has no cause, although in some paths it may come right after (--> c d).
(s/defsession ::protocol-non-trivial-incorrect []
              (s/par
                [(s/-->> ::a ::b)
                 (s/-->> ::b ::c)
                 (s/-->> ::c ::d)
                 ]
                [(s/-->> ::a ::b)
                 (s/-->> ::b ::c)
                 (s/-->> ::d ::a)
                 ])
              (s/close ::a ::b)
              (s/close ::b ::c)
              (s/close ::c ::d)
              (s/close ::d ::a)
              )