(ns discourje.main
  (:require [discourje.twoBuyersProtocol :refer :all]
            [discourje.Buyer1 :as b1]
            [discourje.Buyer2 :as b2]
            [discourje.Seller :as se]))

;first define the protocol
(def protocol (atom (getProtocol)))
;(println protocol)
(clojure.core.async/thread (b1/orderBook "buyer1" protocol))
(clojure.core.async/thread (b2/orderBook "buyer2" protocol))
(clojure.core.async/thread (se/orderBook "seller" protocol))

