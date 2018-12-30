(ns discourje.mainWithRoles
  (:require [discourje.twoBuyersProtocol :refer :all]
            [discourje.Buyer1 :as b1]
            [discourje.Buyer2 :as b2]
            [discourje.Seller :as se]))

;first define the protocol
(def protocol (atom (getProtocol)))
;define the participants on the protocol
(def seller (discourje.core.core/->participant "seller" protocol))
(def buyer1 (discourje.core.core/->participant "buyer1" protocol))
(def buyer2 (discourje.core.core/->participant "buyer2" protocol))
;(println protocol)
(clojure.core.async/thread (se/orderBookParticipant seller))
(clojure.core.async/thread (b1/orderBookParticipant buyer1))
(clojure.core.async/thread (b2/orderBookParticipant buyer2))

