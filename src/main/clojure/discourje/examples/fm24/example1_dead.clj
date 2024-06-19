(ns discourje.examples.fm24.example1-dead
  (:require [clojure.test :refer [deftest]]
            [discourje.core.async :refer :all]
            [discourje.core.spec :as s]))

(s/defrole ::buyer1)
(s/defrole ::buyer2)
(s/defrole ::seller)

(s/defsession ::two-buyer []
  (s/cat
   (s/-->> String ::buyer1 ::seller)
   (s/par
    (s/cat
     (s/-->> Double ::seller ::buyer1)
     (s/-->> Double ::buyer1 ::buyer2))
    (s/-->> Double ::seller ::buyer2))
   (s/-->> Boolean ::buyer2 ::seller)))

(def c1 (chan 1))
(def c2 (chan 1))
(def c3 (chan 1))
(def c4 (chan 1))
(def c5 (chan 1))
(def c6 (chan 1))

(def m (monitor ::two-buyer :n 3))
(link c1 (s/role ::buyer1) (s/role ::buyer2) m)
(link c2 (s/role ::buyer1) (s/role ::seller) m)
(link c3 (s/role ::buyer2) (s/role ::buyer1) m)
(link c4 (s/role ::buyer2) (s/role ::seller) m)
(link c5 (s/role ::seller) (s/role ::buyer1) m)
(link c6 (s/role ::seller) (s/role ::buyer2) m)

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