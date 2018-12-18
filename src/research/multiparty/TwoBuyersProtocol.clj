(ns research.multiparty.TwoBuyersProtocol
  (:require [research.multiparty.monitoring :as mon :refer :all]
            [research.multiparty.core :refer :all]))


(def channels (generateChannels ["buyer1" "buyer2" "seller"]))

(defn- defineProtocol []
  (vector
    (->monitor "title" "buyer1" "seller")
    (->monitor "quote" "seller" {"buyer1" "buyer2"})
    (->monitor "quoteDiv" "buyer1" "buyer2")
    (->choice (->monitor "ok" "buyer2" "seller") (->monitor "quit" "buyer2" "seller")
              [(->monitor "address" "buyer2" "seller")
               (->monitor "date" "seller" "buyer2")]
              [(->monitor "quit" "buyer2" "seller")])))

(def protocol (atom (defineProtocol)))
(def activeMonitor (atom {}))

(mon/setActiveMonitor activeMonitor protocol)

(defn communicate
  ([action value from to]                                   ; send
   (mon/tryCommunicate action value from to channels activeMonitor protocol))
  ([action from to]                                         ; listen
   (mon/tryCommunicate action from to channels activeMonitor protocol)))