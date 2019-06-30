(ns discourje.TwoBuyerProtocolExceptions.twoBuyersProtocol
  (require [discourje.core.async :refer :all]
           [discourje.core.logging :refer :all]
           [discourje.TwoBuyerProtocolExceptions.Buyer1 :as b1]
           [discourje.TwoBuyerProtocolExceptions.Buyer2 :as b2]
           [discourje.TwoBuyerProtocolExceptions.Seller :as s]
           [discourje.core.logging :refer :all]))

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
            (-->> "date" "seller" ["buyer2" "buyer1"])
            (continue :order-book)]
           [(-->> "quit" "buyer2" "seller")]))))

(def b1-s (chan "buyer1" "seller" 2))
(def s-b1 (chan "seller" "buyer1" 2))
(def b1-b2(chan  "buyer1" "buyer2" 2))
(def s-b2 (chan "seller" "buyer2" 2))
(def b2-s (chan "buyer2" "seller" 2))

;generate the infra structure for the protocol
(def infrastructure (add-infrastructure two-buyer-protocol [b1-s s-b1 b1-b2 s-b2 b2-s]))
(set-logging-and-exceptions)
;start each participant on another thread
(thread (b1/order-book infrastructure))
(thread (b2/order-book infrastructure))
(thread (s/order-book infrastructure))