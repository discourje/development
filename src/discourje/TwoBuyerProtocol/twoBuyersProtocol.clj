(ns discourje.TwoBuyerProtocol.twoBuyersProtocol
  (require [discourje.core.async :refer :all]
           [discourje.core.logging :refer :all]
           [discourje.TwoBuyerProtocol.Buyer1 :as b1]
           [discourje.TwoBuyerProtocol.Buyer2 :as b2]
           [discourje.TwoBuyerProtocol.Seller :as s]))

;define two buyer protocol, Notice: it is extended with recursion!
(def two-buyer-protocol
  (mep
    (rec :order-book
         (-->> "title" "buyer1" "seller")
         (-->> "quote" "seller" ["buyer1" "buyer2"])
         (-->> "quote-div" "buyer1" "buyer2")
         (choice
           [(-->> "ok" "buyer2" "seller")
            (-->> "address" "buyer2" "seller")
            (-->> "date" "seller" ["buyer1" "buyer2"])
            (continue :order-book)]
           [(-->> "quit" "buyer2" "seller")
            (close "buyer1" "seller")
            (close "buyer1" "buyer2")
            (close "seller" "buyer1")
            (close "seller" "buyer2")
            (close "buyer2" "seller")]))))

;generate the infra structure for the protocol
(def infrastructure (add-infrastructure two-buyer-protocol))
(set-logging-and-exceptions)
;start each participant on another thread
(thread (b1/order-book infrastructure))
(thread (b2/order-book infrastructure))
(thread (s/order-book infrastructure))