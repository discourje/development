(ns discourje.core.validation.unclosed-channels
  (:require [discourje.core.spec :as s]))

(s/defrole ::a)
(s/defrole ::b)
(s/defrole ::c)
(s/defrole ::d)

;; All used channels are closed
(s/defsession ::protocol-trivial-correct []
              [
               (s/--> ::a ::b)
               (s/--> ::b ::a)
               (s/close ::a ::b)
               (s/close ::b ::a)
               ])

;; channel b a is not closed
(s/defsession ::protocol-trivial-incorrect []
              [
               (s/--> ::a ::b)
               (s/--> ::b ::a)
               (s/close ::a ::b)
               ])

;; all used channels are closed, but only in the path they are used and after some other actions
(s/defsession ::protocol-non-trivial-correct []
              (s/alt
                [
                 (s/--> ::a ::b)
                 (s/--> ::b ::c)
                 (s/close ::b ::c)
                 ]
                [
                 (s/--> ::a ::b)
                 (s/--> ::b ::d)
                 (s/close ::b ::d)
                 ])
              (s/close ::a ::b)
              )

;; channel d a is not closed
(s/defsession ::protocol-non-trivial-incorrect []
              (s/alt
                [(s/--> ::a ::b)
                 (s/--> ::b ::c)
                 (s/--> ::c ::a)
                 ]
                [(s/--> ::a ::c)
                 (s/--> ::c ::d)
                 (s/--> ::d ::a)
                 ])
              (s/close ::a ::b)
              (s/close ::b ::c)
              (s/close ::c ::a)
              )