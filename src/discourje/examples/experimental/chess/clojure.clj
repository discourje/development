(ns discourje.examples.experimental.chess.clojure
  (require [clojure.core.async :refer [thread chan >!! <!! close!]]))

;; Channels

(def w->b (chan 1))
(def b->w (chan 1))
(def b<-w w->b)
(def w<-b b->w)

;; Threads

(load "threads")