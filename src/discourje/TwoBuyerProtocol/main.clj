(ns discourje.TwoBuyerProtocol.main
  (:require [discourje.TwoBuyerProtocol.twoBuyersProtocol :refer :all]
            [discourje.TwoBuyerProtocol.Buyer1 :as b1]
            [discourje.TwoBuyerProtocol.Buyer2 :as b2]
            [discourje.TwoBuyerProtocol.Seller :as se]))

;first define the protocol
(def protocol (atom (getProtocol)))
;(println protocol)
(clojure.core.async/thread (se/orderBook "seller" protocol))
(clojure.core.async/thread (b1/orderBook "buyer1" protocol))
(clojure.core.async/thread (b2/orderBook "buyer2" protocol))

