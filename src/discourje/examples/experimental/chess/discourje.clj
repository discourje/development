(ns discourje.examples.experimental.chess.discourje
  (require [discourje.examples.experimental.dsl :refer :all]
           [discourje.examples.experimental.api :refer :all]))

(def white (role "white"))
(def black (role "black"))

;(def chess-close (--> white black Long))
;(def chess (fix :X [(--> white black String)
;                    (alt chess-close
;                         [(--> black white String)
;                          (alt chess-close
;                               (fix :X))])]))

(def chess (fix :X [(--> white black String)
                    (--> black white String)
                    (fix :X)]))

(def m (monitor (spec chess)))

(def w->b (chan 1 white black m))
(def b->w (chan 1 black white m))
(def b<-w w->b)
(def w<-b b->w)

(load "threads")