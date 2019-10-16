(ns discourje.examples.tacas2020.misc.chess.discourje
  (require [discourje.core.async :refer :all]))

;;
;; Configuration
;;

(enable-wildcard)
(discourje.core.logging/set-logging-exceptions)
(discourje.core.logging/set-throwing true)

;;
;; Roles
;;

(def white (role "white"))
(def black (role "black"))

;;
;; Specification
;;

(def chess-close (dsl (par (-## white black) (-## black white))))

(def chess (dsl (fix :X [(--> white black String)
                         (alt (ins chess-close)
                              [(--> black white String)
                               (alt (ins chess-close)
                                    (fix :X))])])))

;;
;; Implementation
;;

(def m (moni (spec chess)))

(def w->b (chan 1 white black m))
(def b->w (chan 1 black white m))
(def b<-w w->b)
(def w<-b b->w)

(load "threads")