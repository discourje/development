(ns discourje.core.ctl.performance
  (:require [discourje.core.spec :as s]
            [discourje.core.spec.lts :as lts]))

(s/defrole ::a)
(s/defrole ::b)
(s/defrole ::c)
(s/defrole ::d)

(s/defsession ::large-lts [size]
              (s/par
                (s/cat-every
                  [i (range size)]
                  [(s/--> String (::a i) (::a (+ i 1)))
                   (s/close (::a i) (::a (+ i 1)))
                   ])
                (s/cat-every
                  [i (range size)]
                  [(s/--> String (::b i) (::b (+ i 1)))
                   (s/close (::b i) (::b (+ i 1)))
                   ])
                (s/cat-every
                  [i (range size)]
                  [(s/--> String (::c i) (::c (+ i 1)))
                   (s/close (::c i) (::c (+ i 1)))
                   ])
                ))

(defn get-large-lts [size]
  (lts/lts (s/session ::large-lts [size])))