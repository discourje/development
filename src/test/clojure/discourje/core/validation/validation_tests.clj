(ns discourje.core.validation.validation-tests
  (:require [clojure.test :refer :all]
            [discourje.core.spec :as s]
            [discourje.core.spec.lts :as lts]
            [discourje.core.validation.causality :as c]
            [discourje.core.validation.close-multiple-times :as cmt]
            [discourje.core.validation.close-unmentioned-channel :as cuc]
            [discourje.core.validation.message-to-self :as mts]
            [discourje.core.validation.send-after-close :as sac]
            [discourje.core.validation.unclosed-channels :as uc]
            )
  (:import (discourje.core.validation DiscourjeModel)))

(s/defrole ::white)
(s/defrole ::black)

(defn print-lts [lts]
  (let [dm (new DiscourjeModel lts)
        lts-size (.size (.getStates lts))
        dm-size (.size (.getStates dm))]
    (print (str "lts-size: " lts-size "; dm-size: " dm-size "\n"))
    (is (> lts-size 0))
    )
  )

(defn make-lts [protocol vars]
  (lts/lts (s/session protocol vars) :on-the-fly false :history true))

(def lts-list (list
                (make-lts ::c/protocol-trivial-correct [])
                (make-lts ::c/protocol-trivial-incorrect [])
                (make-lts ::c/protocol-non-trivial-correct [])
                (make-lts ::c/protocol-non-trivial-incorrect [])
                (make-lts ::cmt/protocol-trivial-correct [])
                (make-lts ::cmt/protocol-trivial-incorrect [])
                (make-lts ::cmt/protocol-non-trivial-correct [])
                (make-lts ::cmt/protocol-non-trivial-incorrect [])
                (make-lts ::cuc/protocol-trivial-correct [])
                (make-lts ::cuc/protocol-trivial-incorrect [])
                (make-lts ::cuc/protocol-non-trivial-correct [])
                (make-lts ::cuc/protocol-non-trivial-incorrect [])
                (make-lts ::mts/protocol-trivial-correct [])
                (make-lts ::mts/protocol-trivial-incorrect [])
                (make-lts ::mts/protocol-non-trivial-correct [])
                (make-lts ::mts/protocol-non-trivial-incorrect [])
                (make-lts ::sac/protocol-trivial-correct [])
                (make-lts ::sac/protocol-trivial-incorrect [])
                (make-lts ::sac/protocol-non-trivial-correct [true])
                (make-lts ::sac/protocol-non-trivial-incorrect [])
                (make-lts ::uc/protocol-trivial-correct [])
                (make-lts ::uc/protocol-trivial-incorrect [])
                (make-lts ::uc/protocol-non-trivial-correct [])
                (make-lts ::uc/protocol-non-trivial-incorrect [])))

(deftest validite-test-cases-syntax
  (dorun (map print-lts lts-list))
  (is true)
  )

(validite-test-cases-syntax)