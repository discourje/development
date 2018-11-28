(ns discourje.multiparty.twoBuyers
  (:import (discourje.multiparty.core monitor)))

(defrecord protocolInstance [channels protocol activeMonitor])
(defn- defineProtocol []
  (vector
    (monitor. "title" "buyer1" "seller")
    (discourje.multiparty.core/->monitor "quote" "seller" {"buyer1" "buyer2"})
    (discourje.multiparty.core/->monitor "quoteDiv" "buyer1" "buyer2")
    (discourje.multiparty.core/->choice (discourje.multiparty.core/->monitor "ok" "buyer2" "seller") (discourje.multiparty.core/->monitor "quit" "buyer2" "seller")
              [(discourje.multiparty.core/->monitor "address" "buyer2" "seller")
               (discourje.multiparty.core/->monitor "date" "seller" "buyer2")]
              [(discourje.multiparty.core/->monitor "quit" "buyer2" "seller")])))
(defn getProtocol
  "generate the protocol, channels and set the first monitor active"
  []
  (let [monitors (defineProtocol)
        prot (->protocolInstance (discourje.multiparty.core/generateChannels ["buyer1" "buyer2" "seller"]) monitors nil)]
    (discourje.multiparty.monitor/activateNextMonitor prot)
    prot))


(def protocol (atom (getProtocol)))