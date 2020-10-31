(ns discourje.core.validation.message-to-self
  (:require [discourje.core.spec :as s]))

(s/defrole ::a)
(s/defrole ::b)
(s/defrole ::c)
(s/defrole ::d)

;; No message to self
(s/defsession ::protocol-trivial-correct []
              [
               (s/--> ::a ::b)
               (s/--> ::b ::a)
               (s/close ::a ::b)
               (s/close ::b ::a)
               ])

;; Message to self
(s/defsession ::protocol-trivial-incorrect []
              [
               (s/--> ::a ::b)
               (s/--> ::b ::b)
               (s/close ::a ::b)
               (s/close ::b ::b)
               ])

(defn send-and-close [r1 r2]
  (s/--> (r1) (r2))
  )

; Roles are not given as literals, but as variables passed as parameters
(s/defsession ::protocol-non-trivial-correct []
              (s/let
                [r1 ::a
                 r2 ::b
                 r3 ::c])
              (s/cat (send-and-close ::a ::b)
                     (send-and-close ::b ::a))
              )

;; Roles are not given as literals, but as variables passed as parameters
(s/defsession ::protocol-non-trivial-incorrect []
              (s/let
                [r1 ::a
                 r2 ::b
                 r3 ::a]
                [(send-and-close ::a ::b)
                 (send-and-close ::a ::a)]
                )
              )
