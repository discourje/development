(ns discourje.multi.main
  (:require [discourje.multiparty.monitoring]
            [discourje.multi.twoBuyers :refer :all]))

;first define the protocol
(def protocol (atom (getProtocol)))

(discourje.multi.Buyer1/orderBook protocol)