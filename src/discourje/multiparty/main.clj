(ns discourje.multiparty.main
  (:require [discourje.multiparty.Buyer1]
            [discourje.multiparty.Buyer2]
            [discourje.multiparty.seller]
            [discourje.multiparty.monitoring]))

;first define the protocol
(def protocol (atom (discourje.multiparty.TwoBuyersProtocol/getProtocol)))

(discourje.multiparty.Buyer1/order protocol)