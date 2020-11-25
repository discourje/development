(ns discourje.core.validation.performance
  (:require [discourje.core.spec :as s]))

(s/defrole ::a)
(s/defrole ::b)
(s/defrole ::c)

(s/defsession ::huge-lts []
              (let [ids (set (range 4))]
                (s/par-every [i ids]
                             [(s/--> String (::a i) (::b i))
                              (s/--> String (::b i) (::c i))
                              (s/close (::a i) (::b i))
                              (s/close (::b i) (::c i))
                              ]
                             )))
