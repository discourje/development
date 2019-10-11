(ns discourje.examples.tacas2020.misc.ttt.clojure
  (require [clojure.core.async :refer [>!! <!! close! chan thread]]))

;; Channels

(def a->b (chan 1))
(def b->a (chan 1))
(def b<-a a->b)
(def a<-b b->a)

;; Threads

(load "threads")