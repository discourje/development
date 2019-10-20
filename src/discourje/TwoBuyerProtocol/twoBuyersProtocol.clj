(ns discourje.TwoBuyerProtocol.twoBuyersProtocol
  (require [discourje.core.async :refer :all]
           [discourje.core.logging :refer :all]
           [discourje.TwoBuyerProtocol.Buyer1 :as b1]
           [discourje.TwoBuyerProtocol.Buyer2 :as b2]
           [discourje.TwoBuyerProtocol.Seller :as s])
  (:import (clojure.lang PersistentArrayMap)
           (java.util GregorianCalendar)))
;(def two-buyer-protocol
;  (mep
;    (-->> String "buyer1" "seller")
;    (-->> Long "seller" ["buyer1" "buyer2"])
;    (-->> Long "buyer1" "buyer2")
;    (choice
;      [(-->> PersistentArrayMap "buyer2" "seller")
;       (-->> String "buyer2" "seller")
;       (-->> Long "seller" "buyer2")]
;      [(-->> PersistentArrayMap "buyer2" "seller")])))
;define two buyer protocol, Notice: it is extended with recursion!
(def two-buyer-protocol
  (mep
    (rec :order-book
         (-->> String "buyer1" "seller")
         (-->> Long "seller" ["buyer1" "buyer2"])
         (-->> Long "buyer1" "buyer2")
         (choice
           [(-->> PersistentArrayMap "buyer2" "seller")
            (-->> GregorianCalendar "buyer2" "seller")
            (-->> String "seller" ["buyer1" "buyer2"])
            (continue :order-book)]
           [(-->> PersistentArrayMap "buyer2" "seller")
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