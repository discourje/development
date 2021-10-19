(ns discourje.core.ctl.closed-channel-must-be-used-in-path-async
  (:require [discourje.core.spec :as s]))

(s/defrole ::a)
(s/defrole ::b)
(s/defrole ::c)
(s/defrole ::d)

;; All closed channels are also used on that path
(s/defsession ::protocol-trivial-correct []
              (s/alt
                [(s/-->> ::a ::b)
                 (s/close ::a ::b)]
                [(s/-->> ::a ::c)
                 (s/close ::a ::c)]))

; Channel and a-->c is not used on the path.
(s/defsession ::protocol-trivial-incorrect []
              (s/-->> ::a ::b)
              (s/close ::a ::b)
              (s/close ::a ::c))

; Channels closed on parallel paths, just as messages. Every path should, however, contain close and send
(s/defsession ::protocol-non-trivial-correct []
              (s/par
                (s/-->> ::a ::b)
                (s/-->> ::b ::a))
              (s/par
                (s/close ::a ::b)
                (s/close ::b ::a)))

; Closing  channel a to c in the second branch of the first s/alt is not warranted.
(s/defsession ::protocol-non-trivial-incorrect []
              (s/alt
                [(s/alt
                   (s/--> ::a ::b)
                   (s/--> ::a ::c))
                 (s/close ::a ::b)
                 (s/close ::a ::c)]
                [(s/alt
                   (s/--> ::a ::b)
                   (s/--> ::a ::d))
                 (s/close ::a ::b)
                 (s/close ::a ::c)
                 (s/close ::a ::d)]
                )
              )
