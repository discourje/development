(ns discourje.TwoBuyerProtocol.twoBuyersProtocol
  (require [discourje.core.async :refer :all]
           [discourje.core.logging :refer :all]
           [discourje.TwoBuyerProtocol.Buyer1 :as b1]
           [discourje.TwoBuyerProtocol.Buyer2 :as b2]
           [discourje.TwoBuyerProtocol.Seller :as s]))

;define two buyer protocol, Notice: it is extended with recursion!
(defn two-buyer-protocol []
  (mep (rec :order-book
            (-->> "title" "buyer1" "seller")
            (-->> "quote" "seller" ["buyer1" "buyer2"])
            (-->> "quote-div" "buyer1" "buyer2")
            (choice
              [(-->> "ok" "buyer2" "seller")
               (-->> "date" "seller" "buyer2")
               (-->> "repeat" "buyer1" ["buyer1" "seller"])
               (continue :order-book)]
              [(-->> "quit" "buyer2" "seller")]))))

;generate the infra structure for the protocol
(def infrastructure (add-infrastructure (two-buyer-protocol)))

;start each participant on another thread
(clojure.core.async/thread (b2/order-book infrastructure))
(clojure.core.async/thread (b1/order-book infrastructure))
(clojure.core.async/thread (s/order-book infrastructure))
