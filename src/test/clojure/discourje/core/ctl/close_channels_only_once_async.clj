(ns discourje.core.validation.close-channels-only-once-async
  (:require [discourje.core.spec :as s]))

(s/defrole ::a)
(s/defrole ::b)
(s/defrole ::c)
(s/defrole ::d)

;; channels are only closed once
(s/defsession ::protocol-trivial-correct []
              [(s/-->> ::a ::b)
               (s/-->> ::b ::c)
               (s/close ::a ::b)
               (s/close ::b ::c)
               ])

;; channel a b is closed twice
(s/defsession ::protocol-trivial-incorrect []
              [(s/-->> ::a ::b)
               (s/-->> ::b ::c)
               (s/close ::a ::b)
               (s/close ::b ::c)
               (s/close ::a ::b)
               ])

;; channel a b is closed on both paths of an s/alt
(s/defsession ::protocol-non-trivial-correct []
              (s/alt
                [(s/-->> ::a ::b)
                 (s/-->> ::b ::c)
                 (s/close ::a ::b)
                 (s/close ::b ::c)
                 ]
                [(s/-->> ::a ::b)
                 (s/-->> ::b ::d)
                 (s/close ::a ::b)
                 (s/close ::b ::d)
                 ]))

;; channel a b is closed on both paths of an s/par
(s/defsession ::protocol-non-trivial-incorrect []
              (s/par
                [(s/-->> ::a ::b)
                 (s/-->> ::b ::c)
                 (s/close ::a ::b)
                 (s/close ::b ::c)
                 ]
                [(s/-->> ::a ::b)
                 (s/-->> ::b ::d)
                 (s/close ::a ::b)
                 (s/close ::b ::d)
                 ]))