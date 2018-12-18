(ns discourje.main
  (:require [discourje.twoBuyersProtocol :refer :all]
            [discourje.Buyer1 :as b1]
            [discourje.Buyer2 :as b2]
            [discourje.Seller :as se]))

;first define the protocol
(def protocol (atom (getProtocol)))
;(println protocol)
;(b1/orderBook "buyer1" protocol)
;(b2/orderBook "buyer2" protocol)
;(se/orderBook "seller" protocol)