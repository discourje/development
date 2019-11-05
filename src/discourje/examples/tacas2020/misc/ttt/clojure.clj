(ns discourje.examples.tacas2020.misc.ttt.clojure
  (:require [clojure.core.async :refer [>!! <!! close! chan thread]]))

;;
;; Implementation
;;

(def a->b (chan 1))
(def b->a (chan 1))
(def b<-a a->b)
(def a<-b b->a)

(load "threads")