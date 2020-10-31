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
            ))

(s/defrole ::white)
(s/defrole ::black)

(defn print-lts [protocol vars]
  (let [lts (lts/lts (s/session protocol vars) :on-the-fly false :history true)
        lts-size (.size (.getStates lts))]
    (print (str protocol "; #states: " lts-size "\n"))
    (is (> lts-size 0) )
    )
  )

(deftest validite-test-cases-syntax

  (print-lts ::c/protocol-trivial-correct [])
  (print-lts ::c/protocol-trivial-incorrect [])
  (print-lts ::c/protocol-non-trivial-correct [])
  (print-lts ::c/protocol-non-trivial-incorrect [])
  (print-lts ::cmt/protocol-trivial-correct [])
  (print-lts ::cmt/protocol-trivial-incorrect [])
  (print-lts ::cmt/protocol-non-trivial-correct [])
  (print-lts ::cmt/protocol-non-trivial-incorrect [])
  (print-lts ::cuc/protocol-trivial-correct [])
  (print-lts ::cuc/protocol-trivial-incorrect [])
  (print-lts ::cuc/protocol-non-trivial-correct [])
  (print-lts ::cuc/protocol-non-trivial-incorrect [])
  (print-lts ::mts/protocol-trivial-correct [])
  (print-lts ::mts/protocol-trivial-incorrect [])
  (print-lts ::mts/protocol-non-trivial-correct [])
  (print-lts ::mts/protocol-non-trivial-incorrect [])
  (print-lts ::sac/protocol-trivial-correct [])
  (print-lts ::sac/protocol-trivial-incorrect [])
  (print-lts ::sac/protocol-non-trivial-correct [true])
  (print-lts ::sac/protocol-non-trivial-incorrect [])
  (print-lts ::uc/protocol-trivial-correct [])
  (print-lts ::uc/protocol-trivial-incorrect [])
  (print-lts ::uc/protocol-non-trivial-correct [])
  (print-lts ::uc/protocol-non-trivial-incorrect [])
  (is true)
  )

(validite-test-cases-syntax)