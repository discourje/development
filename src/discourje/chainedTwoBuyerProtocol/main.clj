(ns discourje.chainedTwoBuyerProtocol.main
  (:require [discourje.chainedTwoBuyerProtocol.twoBuyersProtocol :refer :all]
            [discourje.api.api :refer :all]
            [discourje.chainedTwoBuyerProtocol.Buyer1 :as b1]
            [discourje.chainedTwoBuyerProtocol.Buyer2 :as b2]
            [discourje.chainedTwoBuyerProtocol.Seller :as se]))

;first define the protocol
(def protocol (getProtocol))
;define the participants on the protocol
;we could also define the participants inside the functions too.
(def seller (generateParticipant "seller" protocol))
(def buyer1 (generateParticipant "buyer1" protocol))
(def buyer2 (generateParticipant "buyer2" protocol))

(clojure.core.async/thread (b1/orderBook buyer1))
(clojure.core.async/thread (b2/orderBook buyer2))
(clojure.core.async/thread (se/orderBook seller))

;(println (:activeMonitor @protocol))