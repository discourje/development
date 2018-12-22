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
                 (->monitor "title" "buyer1" "seller")
                 (->monitor "quote" "seller" ["buyer1" "buyer2"])
                 (->monitor "quoteDiv" "buyer1" "buyer2")
                 (->choice [(->monitor "ok" "buyer2" "seller")
                            (->monitor "address" "buyer2" "seller")
                            (->monitor "date" "seller" "buyer2")
                            (->monitor "repeat" "seller" ["buyer2" "buyer1"])
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