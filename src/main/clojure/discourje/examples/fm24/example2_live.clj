(ns discourje.examples.fm24.example2-live
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
    (par
     (cat
      (-->> Long :b :s1)
      (--> Long :s1 :c))
     (close :b :s2))
    (par
     (cat
      (-->> Long :b :s2)
      (--> Long :s2 :c))
     (close :b :s1)))))

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
  (let [x (<!! c1)
        [_ c] (alts!! [[c4 x]
                       [c5 x]])]
    (when (= c c4) (close! c5))
    (when (= c c5) (close! c4)))) ;; FIXED

(thread ;; Client
  (>!! c1 5)
  (alts!! [c2 c3]))

(thread ;; Server1
  (let [x (<!! c4) ;; FIXED
        y (when x (inc x))]
    (when y (>!! c2 y))))

(thread ;; Server2
  (let [x (<!! c5) ;; FIXED
        y (when x (inc x))]
    (when y (>!! c3 y))))