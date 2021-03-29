(ns discourje.core.validation.closed-channel-must-be-used-in-protocol
  (:require [discourje.core.spec :as s]))

(s/defrole ::a)
(s/defrole ::b)
(s/defrole ::c)
(s/defrole ::d)

;; All closed channels are also used
(s/defsession ::protocol-trivial-correct []
              [
               (s/--> ::a ::b)
               (s/--> ::b ::a)
               (s/close ::a ::b)
               (s/close ::b ::a)
               ])

;; Not all closed channels are also used
(s/defsession ::protocol-trivial-incorrect []
              [
               (s/--> ::a ::b)
               (s/close ::a ::b)
               (s/close ::b ::a)
               ])

;; Channels are closed on both paths, but used on only one
(s/defsession ::protocol-non-trivial-correct []
              (s/alt
                [(s/--> ::a ::b)
                 (s/close ::a ::b)
                 (s/close ::b ::a)
                 ]
                [(s/--> ::b ::a)
                 (s/close ::b ::a)
                 (s/close ::a ::b)
                 ]
                ))

;; channel b c is closed, but not used, not even on other paths
(s/defsession ::protocol-non-trivial-incorrect []
              (s/alt
                [(s/--> ::a ::b)
                 (s/close ::a ::b)
                 (s/close ::b ::c)
                 ]
                [(s/--> ::b ::a)
                 (s/close ::a ::b)
                 (s/close ::b ::a)
                 ]
                ))
