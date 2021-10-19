(ns discourje.core.ctl.do-not-send-after-close
  (:require [discourje.core.spec :as s]))

(s/defrole ::a)
(s/defrole ::b)
(s/defrole ::c)
(s/defrole ::d)

;; No channel usage after close
(s/defsession ::protocol-trivial-correct []
              [
               (s/--> ::a ::b)
               (s/--> ::b ::a)
               (s/close ::a ::b)
               (s/close ::b ::a)
               ])

;; channel a b is used after close
(s/defsession ::protocol-trivial-incorrect []
              [
               (s/--> ::a ::b)
               (s/close ::a ::b)
               (s/--> ::b ::a)
               (s/close ::b ::a)
               (s/--> ::a ::b)
               ])

;; the second (--> a b) is only sent if the first (close a b) is not executed
(s/defsession ::protocol-non-trivial-correct
              [test]
              (s/if test
                [(s/--> ::a ::b)
                 (s/close ::a ::b)
                 ]
                [(s/--> ::a ::b)
                 (s/close ::a ::b)
                 ])
              )

;; (close a b) in one path is not guaranteed to happen before (--> a b) in the other path
(s/defsession ::protocol-non-trivial-incorrect []
              (s/par
                [(s/--> ::a ::b)
                 (s/--> ::b ::c)
                 (s/--> ::c ::a)
                 (s/close ::a ::b)
                 ]
                [(s/--> ::a ::b)
                 (s/--> ::b ::d)
                 (s/--> ::d ::a)
                 (s/close ::a ::b)
                 ])
              (s/close ::b ::c)
              (s/close ::c ::a)
              (s/close ::d ::a)
              )