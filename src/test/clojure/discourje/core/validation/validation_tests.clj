(ns discourje.core.validation.validation-tests
  (:require [clojure.test :refer :all]
            [discourje.core.spec :as s]
            [discourje.core.spec.lts :as lts]
            [discourje.core.validation.causality :as c]
            [discourje.core.validation.close-channels-only-once :as ccoo]
            [discourje.core.validation.closed-channel-must-be-used-in-protocol :as ccmbuip]
            [discourje.core.validation.do-not-send-to-self :as dnsts]
            [discourje.core.validation.do-not-send-after-close :as dnsac]
            [discourje.core.validation.used-channels-must-be-closed :as ucmbc]
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
(def close-channels-only-once-trivial-correct (make-lts ::ccoo/protocol-trivial-correct []))
(def close-channels-only-once-trivial-incorrect (make-lts ::ccoo/protocol-trivial-incorrect []))
(def close-channels-only-once-non-trivial-correct (make-lts ::ccoo/protocol-non-trivial-correct []))
(def close-channels-only-once-non-trivial-incorrect (make-lts ::ccoo/protocol-non-trivial-incorrect []))
(def closed-channel-must-be-used-in-protocol-trivial-correct (make-lts ::ccmbuip/protocol-trivial-correct []))
(def closed-channel-must-be-used-in-protocol-trivial-incorrect (make-lts ::ccmbuip/protocol-trivial-incorrect []))
(def closed-channel-must-be-used-in-protocol-non-trivial-correct (make-lts ::ccmbuip/protocol-non-trivial-correct []))
(def closed-channel-must-be-used-in-protocol-non-trivial-incorrect (make-lts ::ccmbuip/protocol-non-trivial-incorrect []))
(def do-not-send-to-self-trivial-correct (make-lts ::dnsts/protocol-trivial-correct []))
(def do-not-send-to-self-trivial-incorrect (make-lts ::dnsts/protocol-trivial-incorrect []))
(def do-not-send-to-self-non-trivial-correct (make-lts ::dnsts/protocol-non-trivial-correct []))
(def do-not-send-to-self-non-trivial-incorrect (make-lts ::dnsts/protocol-non-trivial-incorrect []))
(def do-not-send-after-close-trivial-correct (make-lts ::dnsac/protocol-trivial-correct []))
(def do-not-send-after-close-trivial-incorrect (make-lts ::dnsac/protocol-trivial-incorrect []))
(def do-not-send-after-close-non-trivial-correct (make-lts ::dnsac/protocol-non-trivial-correct [true]))
(def do-not-send-after-close-non-trivial-incorrect (make-lts ::dnsac/protocol-non-trivial-incorrect []))
(def used-channels-must-be-closed-trivial-correct (make-lts ::ucmbc/protocol-trivial-correct []))
(def used-channels-must-be-closed-trivial-incorrect (make-lts ::ucmbc/protocol-trivial-incorrect []))
(def used-channels-must-be-closed-non-trivial-correct (make-lts ::ucmbc/protocol-non-trivial-correct []))
(def used-channels-must-be-closed-non-trivial-incorrect (make-lts ::ucmbc/protocol-non-trivial-incorrect []))
