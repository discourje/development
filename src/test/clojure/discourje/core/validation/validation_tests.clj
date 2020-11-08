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

(def causality-trivial-correct (make-lts ::c/protocol-trivial-correct []))
(def causality-trivial-incorrect (make-lts ::c/protocol-trivial-incorrect []))
(def causality-non-trivial-correct (make-lts ::c/protocol-non-trivial-correct []))
(def causality-non-trivial-incorrect (make-lts ::c/protocol-non-trivial-incorrect []))
(def close-only-once-trivial-correct (make-lts ::cmt/protocol-trivial-correct []))
(def close-only-once-trivial-incorrect (make-lts ::cmt/protocol-trivial-incorrect []))
(def close-only-once-non-trivial-correct (make-lts ::cmt/protocol-non-trivial-correct []))
(def close-only-once-non-trivial-incorrect (make-lts ::cmt/protocol-non-trivial-incorrect []))
(def close-used-channels-trivial-correct (make-lts ::cuc/protocol-trivial-correct []))
(def close-used-channels-trivial-incorrect (make-lts ::cuc/protocol-trivial-incorrect []))
(def close-used-channels-non-trivial-correct (make-lts ::cuc/protocol-non-trivial-correct []))
(def close-used-channels-non-trivial-incorrect (make-lts ::cuc/protocol-non-trivial-incorrect []))
(def message-to-self-trivial-correct (make-lts ::mts/protocol-trivial-correct []))
(def message-to-self-trivial-incorrect (make-lts ::mts/protocol-trivial-incorrect []))
(def message-to-self-non-trivial-correct (make-lts ::mts/protocol-non-trivial-correct []))
(def message-to-self-non-trivial-incorrect (make-lts ::mts/protocol-non-trivial-incorrect []))
(def send-after-close-trivial-correct (make-lts ::sac/protocol-trivial-correct []))
(def send-after-close-trivial-incorrect (make-lts ::sac/protocol-trivial-incorrect []))
(def send-after-close-non-trivial-correct (make-lts ::sac/protocol-non-trivial-correct [true]))
(def send-after-close-non-trivial-incorrect (make-lts ::sac/protocol-non-trivial-incorrect []))
(def unclosed-channels-trivial-correct (make-lts ::uc/protocol-trivial-correct []))
(def unclosed-channels-trivial-incorrect (make-lts ::uc/protocol-trivial-incorrect []))
(def unclosed-channels-non-trivial-correct (make-lts ::uc/protocol-non-trivial-correct []))
(def unclosed-channels-non-trivial-incorrect (make-lts ::uc/protocol-non-trivial-incorrect []))
