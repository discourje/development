(ns discourje.multi.main
  (:require [discourje.multiparty.monitoring]
            [discourje.multi.twoBuyers :refer :all]
            [discourje.multi.Buyer1 :as b1]
            [discourje.multi.Buyer2 :as b2]
            [discourje.multi.Seller :as se]))

;first define the protocol
(def protocol (atom (getProtocol)))
;(println protocol)
(b1/orderBook protocol)
(b2/orderBook protocol)
(se/orderBook protocol)
