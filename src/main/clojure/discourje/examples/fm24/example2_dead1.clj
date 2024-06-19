(ns discourje.examples.fm24.example2-dead1
  (:require [clojure.test :refer [deftest]]
            [discourje.core.async :refer :all]
            [discourje.core.spec :as s]))

(s/defrole ::c)
(s/defrole ::b)
(s/defrole ::s1)
(s/defrole ::s2)

(s/defsession ::load-balancer []
  (s/cat
   (s/--> Long ::c ::b)
   (s/alt
    (s/cat
     (s/-->> Long ::b ::s1)
     (s/--> Long ::s1 ::c))
    (s/cat
     (s/-->> Long ::b ::s2)
     (s/--> Long ::s2 ::c)))))

(def c1 (chan))
(def c2 (chan))
(def c3 (chan))
(def c4 (chan 512))
(def c5 (chan 1024))

(def m (monitor ::load-balancer :n 4))
(link c1 (s/role ::c) (s/role ::b) m)
(link c2 (s/role ::s1) (s/role ::c) m)
(link c3 (s/role ::s2) (s/role ::c) m)
(link c4 (s/role ::b) (s/role ::s1) m)
(link c5 (s/role ::b) (s/role ::s2) m)

(thread ;; Load Balancer
  (let [x (<!! c1)]
    (alts!! [[c4 x]
             [c5 x]]))) ;; BUG

(thread ;; Client
  (>!! c1 5)
  (alts!! [c2 c3]))

(thread ;; Server1
  (let [x (<!! c2) ;; BUG
        y (inc x)]
    (>!! c2 y)))

(thread ;; Server2
  (let [x (<!! c3) ;; BUG
        y (inc x)]
    (>!! c3 y)))