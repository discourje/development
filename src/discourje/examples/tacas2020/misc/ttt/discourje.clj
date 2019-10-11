(ns discourje.examples.tacas2020.misc.ttt.discourje
  (require [discourje.core.async :refer :all]
           [discourje.examples.tacas2020.main :refer [bench]]))

;;
;; Configuration
;;

(enable-wildcard)
(discourje.core.logging/set-logging-exceptions)
(discourje.core.logging/set-throwing true)
(reset! <!!-unwrap true)

;; Roles

(def alice (role "alice"))
(def bob (role "bob"))

;; Specification

(def ttt-close (dsl (par (-## alice bob) (-## bob alice))))

(def ttt (dsl (fix :X [(--> alice bob Long)
                       (alt (ins ttt-close)
                            [(--> bob alice Long)
                             (alt (ins ttt-close)
                                  (fix :X))])])))

;; Monitor

(def m (moni (spec ttt)))

;; Channels

(def a->b (chan 1 alice bob m))
(def b->a (chan 1 bob alice m))
(def b<-a a->b)
(def a<-b b->a)

;; Threads

(load "threads")