(ns discourje.multi.twoBuyers
  (:require [discourje.multi.core :refer :all] ))

(defrecord protocolInstance [channels protocol activeMonitor])
(defn- defineProtocol []
  (vector
    (->monitor "title" "buyer1" "seller")
    (->monitor "quote" "seller" {"buyer1" "buyer2"})
    (->monitor "quoteDiv" "buyer1" "buyer2")
    (->choice (->monitor "ok" "buyer2" "seller") (->monitor "quit" "buyer2" "seller")
              [(->monitor "address" "buyer2" "seller")
               (->monitor "date" "seller" "buyer2")]
              [(->monitor "quit" "buyer2" "seller")])))
(defn getProtocol
  "generate the protocol, channels and set the first monitor active"
  []
  (let [monitors (defineProtocol)
        prot (->protocolInstance (discourje.multi.core/generateChannels ["buyer1" "buyer2" "seller"]) monitors nil)]
    (discourje.multi.monitor/activateNextMonitor prot)
    prot))


(def protocol (atom (getProtocol)))