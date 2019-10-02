(ns discourje.examples.experimental.chess.discourje
  (require [discourje.core.async :refer :all]))

;;
;; Configuration
;;

(enable-wildcard)
(discourje.core.logging/set-logging-exceptions)
(discourje.core.logging/set-throwing true)
(reset! <!!-unwrap true)

;; Roles

(def white (role "white"))
(def black (role "black"))

;; Specification

(def chess-close (dsl (par (-## white black) (-## black white))))

(def chess (dsl (fix :X [(--> white black String)
                         (alt chess-close
                              [(--> black white String)
                               (alt chess-close
                                    (fix :X))])])))

;; Monitor

(def m (mon (spec chess)))

;; Channels

(def w->b (chan 1 white black m))
(def b->w (chan 1 black white m))
(def b<-w w->b)
(def w<-b b->w)

;; Threads

(load "threads")