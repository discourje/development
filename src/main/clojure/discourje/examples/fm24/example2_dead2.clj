(ns discourje.examples.fm24.example2-dead2
  (:require [discourje.core.async :refer :all]
            [discourje.core.spec :refer [defthread defsession -->> --> close alt cat par role]]))

(defthread :c)
(defthread :b)
(defthread :s1)
(defthread :s2)

(defsession :load-balancer []
  (cat
   (--> Long :c :b)
   (alt
    (cat
     (-->> Long :b :s1)
     (--> Long :s1 :c))
    (cat
     (-->> Long :b :s2)
     (--> Long :s2 :c)))))

(def c1 (chan))
(def c2 (chan))
(def c3 (chan))
(def c4 (chan 512))
(def c5 (chan 1024))

(def m (monitor :load-balancer :n 4))
(link c1 :c :b m)
(link c2 :s1 :c m)
(link c3 :s2 :c m)
(link c4 :b :s1 m)
(link c5 :b :s2 m)

(thread ;; Load Balancer
  (let [x (<!! c1)]
    (alts!! [[c4 x]
             [c5 x]]))) ;; BUG

(thread ;; Client
  (>!! c1 5)
  (alts!! [c2 c3]))

(thread ;; Server1
  (let [x (<!! c4) ;; FIXED
        y (inc x)]
    (>!! c2 y)))

(thread ;; Server2
  (let [x (<!! c5) ;; FIXED
        y (inc x)]
    (>!! c3 y)))