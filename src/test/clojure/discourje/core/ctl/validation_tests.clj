(ns discourje.core.validation.validation-tests
  (:require [clojure.test :refer :all]
            [discourje.core.spec :as s]
            [discourje.core.spec.lts :as lts]
            [discourje.core.validation.causality :as c]
            [discourje.core.validation.close-channels-only-once :as ccoo]
            [discourje.core.validation.closed-channel-must-be-used-in-protocol :as ccuiprot]
            [discourje.core.validation.closed-channel-must-be-used-in-path :as ccuipath]
            [discourje.core.validation.do-not-send-to-self :as dnsts]
            [discourje.core.validation.do-not-send-after-close :as dnsac]
            [discourje.core.validation.used-channels-must-be-closed :as ucmbc]
            [discourje.core.validation.causality-async :as ca]
            [discourje.core.validation.close-channels-only-once-async :as ccooa]
            [discourje.core.validation.closed-channel-must-be-used-in-protocol-async :as ccuiprota]
            [discourje.core.validation.closed-channel-must-be-used-in-path-async :as ccuipatha]
            [discourje.core.validation.do-not-send-to-self-async :as dnstsa]
            [discourje.core.validation.do-not-send-after-close-async :as dnsaca]
            [discourje.core.validation.used-channels-must-be-closed-async :as ucmbca]
            [discourje.core.validation.performance :as perf]
            )
  (:import (ctl Model)))

(s/defrole ::white)
(s/defrole ::black)

(defn print-lts [lts]
  (let [dm (new Model lts)
        lts-size (.size (.getStates lts))
        dm-size (.size (.getStates dm))]
    (print (str "lts-size: " lts-size "; dm-size: " dm-size "\n"))
    (is (> lts-size 0))
    )
  )

(defn make-lts [protocol vars]
  (lts/lts (s/session protocol vars) :on-the-fly true))

(def causality-trivial-correct (make-lts ::c/protocol-trivial-correct []))
(def causality-trivial-incorrect (make-lts ::c/protocol-trivial-incorrect []))
(def causality-non-trivial-correct (make-lts ::c/protocol-non-trivial-correct []))
(def causality-non-trivial-incorrect (make-lts ::c/protocol-non-trivial-incorrect []))
(def close-channels-only-once-trivial-correct (make-lts ::ccoo/protocol-trivial-correct []))
(def close-channels-only-once-trivial-incorrect (make-lts ::ccoo/protocol-trivial-incorrect []))
(def close-channels-only-once-non-trivial-correct (make-lts ::ccoo/protocol-non-trivial-correct []))
(def close-channels-only-once-non-trivial-incorrect (make-lts ::ccoo/protocol-non-trivial-incorrect []))
(def closed-channel-must-be-used-in-protocol-trivial-correct (make-lts ::ccuiprot/protocol-trivial-correct []))
(def closed-channel-must-be-used-in-protocol-trivial-incorrect (make-lts ::ccuiprot/protocol-trivial-incorrect []))
(def closed-channel-must-be-used-in-protocol-non-trivial-correct (make-lts ::ccuiprot/protocol-non-trivial-correct []))
(def closed-channel-must-be-used-in-protocol-non-trivial-incorrect (make-lts ::ccuiprot/protocol-non-trivial-incorrect []))
(def closed-channel-must-be-used-in-path-trivial-correct (make-lts ::ccuipath/protocol-trivial-correct []))
(def closed-channel-must-be-used-in-path-trivial-incorrect (make-lts ::ccuipath/protocol-trivial-incorrect []))
(def closed-channel-must-be-used-in-path-non-trivial-correct (make-lts ::ccuipath/protocol-non-trivial-correct []))
(def closed-channel-must-be-used-in-path-non-trivial-incorrect (make-lts ::ccuipath/protocol-non-trivial-incorrect []))
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

; ASYNC
(def causality-trivial-correct-async (make-lts ::ca/protocol-trivial-correct []))
(def causality-trivial-incorrect-async (make-lts ::ca/protocol-trivial-incorrect []))
(def causality-non-trivial-correct-async (make-lts ::ca/protocol-non-trivial-correct []))
(def causality-non-trivial-incorrect-async (make-lts ::ca/protocol-non-trivial-incorrect []))
(def close-channels-only-once-trivial-correct-async (make-lts ::ccooa/protocol-trivial-correct []))
(def close-channels-only-once-trivial-incorrect-async (make-lts ::ccooa/protocol-trivial-incorrect []))
(def close-channels-only-once-non-trivial-correct-async (make-lts ::ccooa/protocol-non-trivial-correct []))
(def close-channels-only-once-non-trivial-incorrect-async (make-lts ::ccooa/protocol-non-trivial-incorrect []))
(def closed-channel-must-be-used-in-protocol-trivial-correct-async (make-lts ::ccuiprota/protocol-trivial-correct []))
(def closed-channel-must-be-used-in-protocol-trivial-incorrect-async (make-lts ::ccuiprota/protocol-trivial-incorrect []))
(def closed-channel-must-be-used-in-protocol-non-trivial-correct-async (make-lts ::ccuiprota/protocol-non-trivial-correct []))
(def closed-channel-must-be-used-in-protocol-non-trivial-incorrect-async (make-lts ::ccuiprota/protocol-non-trivial-incorrect []))
(def closed-channel-must-be-used-in-path-trivial-correct-async (make-lts ::ccuipatha/protocol-trivial-correct []))
(def closed-channel-must-be-used-in-path-trivial-incorrect-async (make-lts ::ccuipatha/protocol-trivial-incorrect []))
(def closed-channel-must-be-used-in-path-non-trivial-correct-async (make-lts ::ccuipatha/protocol-non-trivial-correct []))
(def closed-channel-must-be-used-in-path-non-trivial-incorrect-async (make-lts ::ccuipatha/protocol-non-trivial-incorrect []))
(def do-not-send-to-self-trivial-correct-async (make-lts ::dnstsa/protocol-trivial-correct []))
(def do-not-send-to-self-trivial-incorrect-async (make-lts ::dnstsa/protocol-trivial-incorrect []))
(def do-not-send-to-self-non-trivial-correct-async (make-lts ::dnstsa/protocol-non-trivial-correct []))
(def do-not-send-to-self-non-trivial-incorrect-async (make-lts ::dnstsa/protocol-non-trivial-incorrect []))
(def do-not-send-after-close-trivial-correct-async (make-lts ::dnsaca/protocol-trivial-correct []))
(def do-not-send-after-close-trivial-incorrect-async (make-lts ::dnsaca/protocol-trivial-incorrect []))
(def do-not-send-after-close-non-trivial-correct-async (make-lts ::dnsaca/protocol-non-trivial-correct [true]))
(def do-not-send-after-close-non-trivial-incorrect-async (make-lts ::dnsaca/protocol-non-trivial-incorrect []))
(def used-channels-must-be-closed-trivial-correct-async (make-lts ::ucmbca/protocol-trivial-correct []))
(def used-channels-must-be-closed-trivial-incorrect-async (make-lts ::ucmbca/protocol-trivial-incorrect []))
(def used-channels-must-be-closed-non-trivial-correct-async (make-lts ::ucmbca/protocol-non-trivial-correct []))
(def used-channels-must-be-closed-non-trivial-incorrect-async (make-lts ::ucmbca/protocol-non-trivial-incorrect []))
