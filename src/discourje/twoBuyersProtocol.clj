(ns discourje.twoBuyersProtocol
  (require [discourje.core.monitor :refer :all])
  (use [discourje.core.core :only [generateChannels]]))
;an instance of a protocol consists of a collection of channels, protocol definition and a monitor flagged active
(defrecord protocolInstance [channels protocol activeMonitor template])

(defn- defineProtocol []
  (vector
    (->monitor "title" "buyer1" "seller")
    (->monitor "quote" "seller" ["buyer1" "buyer2"])
    (->monitor "quoteDiv" "buyer1" "buyer2")
    (->choice [(->monitor "ok" "buyer2" "seller")
               (->monitor "address" "buyer2" "seller")
               (->monitor "date" "seller" "buyer2")]
              [(->monitor "quit" "buyer2" "seller")])))

(defn- defineRecurringProtocol []
  (vector (->recursion :x
               (vector
                 ;(->monitor "title" "buyer1" "seller")
                 (->sendM "title" "buyer1" "seller")
                 (->receiveM "title" "seller" "buyer1")
                 (->monitor "quote" "seller" ["buyer1" "buyer2"])
                 (->monitor "quoteDiv" "buyer1" "buyer2")
                 (->choice [
                            (->sendM "ok" "buyer2" "seller")
                            (->sendM "address" "buyer2" "seller")
                            (->receiveM "ok" "seller" "buyer2")
                            (->receiveM "address" "seller" "buyer2")
                            ;(->monitor "ok" "buyer2" "seller")
                            ;(->monitor "address" "buyer2" "seller")
                            (->sendM "date" "seller" "buyer2")
                            (->sendM "repeat" "seller" ["buyer2" "buyer1"])
                            (->receiveM "date" "buyer2" "seller")
                            ;(->monitor "repeat" "seller" ["buyer2" "buyer1"])
                            (->receiveM "repeat" ["buyer2" "buyer1"] "seller")
                            (generateRecur :x)
                            ]
                           [(->monitor "quit" "buyer2" "seller")
                            (generateRecurStop :x)
                            ])))))

(defn getProtocol
  "generate the protocol, channels and set the first monitor active"
  []
  (let [monitors (defineRecurringProtocol)
        prot (->protocolInstance (generateChannels ["buyer1" "buyer2" "seller"]) (atom monitors) (atom nil) monitors)]
    (activateNextMonitor prot)
    prot))