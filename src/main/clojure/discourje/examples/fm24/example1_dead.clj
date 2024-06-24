(ns discourje.examples.fm24.example1-dead
  (:require [discourje.core.async :refer :all]
            [discourje.core.spec :refer [defthread defsession -->> --> close alt cat par role]]))

(defthread :buyer1)
(defthread :buyer2)
(defthread :seller)

(defsession :two-buyer []
  (cat
   (-->> String :buyer1 :seller)
   (par
    (cat
     (-->> Double :seller :buyer1)
     (-->> Double :buyer1 :buyer2))
    (-->> Double :seller :buyer2))
   (-->> Boolean :buyer2 :seller)))

(def c1 (chan 1))
(def c2 (chan 1))
(def c3 (chan 1))
(def c4 (chan 1))
(def c5 (chan 1))
(def c6 (chan 1))

(def m (monitor :two-buyer :n 3))
(link c1 :buyer1 :buyer2 m)
(link c2 :buyer1 :seller m)
(link c3 :buyer2 :buyer1 m)
(link c4 :buyer2 :seller m)
(link c5 :seller :buyer1 m)
(link c6 :seller :buyer2 m)

(thread ;; Buyer1
  (>!! c2 "book")
  (let [x (<!! c3) ;; BUG
        y (/ x 2)]
    (>!! c1 y)))

(thread ;; Buyer2
  (let [x (<!! c6)
        y (<!! c1)
        z (= x y)]
    (>!! c4 z)))

(thread ;; Seller
  (<!! c2)
  (>!! c5 20.00)
  (>!! c6 20.00)
  (println (<!! c4)))