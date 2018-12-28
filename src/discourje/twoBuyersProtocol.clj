(ns discourje.twoBuyersProtocol
  (require [discourje.core.monitor :refer :all])
  (use [discourje.core.core :only [generateChannels]]))
;an instance of a protocol consists of a collection of channels, protocol definition and a monitor flagged active
(defrecord protocolInstance [channels protocol activeMonitor template])

(defn- defineRecurringProtocol []
  (vector (->recursion :x
               (vector
                 (->sendM "title" "buyer1" "seller")
                 (->receiveM "title" "seller" "buyer1")
                 (->sendM "quote" "seller" ["buyer1" "buyer2"])
                 (->receiveM "quote" ["buyer1" "buyer2"] "seller")
                 (->sendM "quoteDiv" "buyer1" "buyer2")
                 (->receiveM "quoteDiv" "buyer2" "buyer1")
                 (->choice [
                            (->sendM "ok" "buyer2" "seller")
                            (->sendM "address" "buyer2" "seller")
                            (->receiveM "ok" "seller" "buyer2")
                            (->receiveM "address" "seller" "buyer2")
                            (->sendM "date" "seller" "buyer2")
                            (->sendM "repeat" "seller" ["buyer2" "buyer1"])
                            (->receiveM "date" "buyer2" "seller")
                            (->receiveM "repeat" ["buyer2" "buyer1"] "seller")
                            (generateRecur :x)
                            ]
                           [
                            (->sendM "quit" "buyer2" "seller")
                            (->receiveM "quit" "seller" "buyer2")
                            (generateRecurStop :x)
                            ])))
))

(defn getProtocol
  "generate the protocol, channels and set the first monitor active"
  []
  (let [monitors (defineRecurringProtocol)
        prot (->protocolInstance (generateChannels ["buyer1" "buyer2" "seller"]) (atom monitors) (atom nil) monitors)]
    (activateNextMonitor prot)
    prot))