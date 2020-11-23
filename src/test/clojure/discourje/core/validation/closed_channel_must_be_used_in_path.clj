(ns discourje.core.validation.closed-channel-must-be-used-in-path
  (:require [discourje.core.spec :as s]))

(s/defrole ::a)
(s/defrole ::b)
(s/defrole ::c)
(s/defrole ::d)

;; All closed channels are also used on that path
(s/defsession ::protocol-trivial-correct []
              (s/alt
                [(s/--> ::a ::b)
                 (s/close ::a ::b)]
                [(s/--> ::a ::c)
                 (s/close ::a ::c)]))

;; Channel a-->b and a-->c are not always used on the path.
(s/defsession ::protocol-trivial-incorrect []
              (s/--> ::a ::b)
              (s/close ::a ::b)
              (s/close ::a ::c))

; Channels closed on parallel paths, just as messages. Every path should, however, contain close and send
(s/defsession ::protocol-non-trivial-correct []
              (s/par
                (s/--> ::a ::b)
                (s/--> ::b ::a))
              (s/par
                (s/close ::a ::b)
                (s/close ::b ::a)))

; For both closed a channel exists where it is used, but also a channel where it is not used
(s/defsession ::protocol-non-trivial-incorrect []
              (s/alt
                (s/--> ::a ::b)
                (s/--> ::a ::c)
                )
              (s/close ::a ::b)
              (s/close ::a ::c))
