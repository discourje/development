(ns discourje.examples.tacas2020.misc.chess.clojure
  (require [clojure.core.async :refer [>!! <!! close! chan thread]]))

;; Channels

(def w->b (chan 1))
(def b->w (chan 1))
(def b<-w w->b)
(def w<-b b->w)

;; Threads

(load "threads")