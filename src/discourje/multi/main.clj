(ns discourje.multi.main
  (:require [discourje.multiparty.Buyer1]
            [discourje.multiparty.Buyer2]
            [discourje.multiparty.seller]
            [discourje.multiparty.monitoring]))

;first define the protocol
(def protocol (atom (discourje.multi.TwoBuyersProtocol/getProtocol)))

(discourje.multi.Buyer1/order protocol)