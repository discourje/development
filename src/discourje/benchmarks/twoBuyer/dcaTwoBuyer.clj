(ns discourje.benchmarks.twoBuyer.dcaTwoBuyer
  (:require [discourje.core.async :refer :all]
            [discourje.core.logging :refer :all])
  (:use [slingshot.slingshot :only [throw+ try+]]))

(defn buyer1 "order a book from buyer1's perspective"
  ([infra]
   (let [b1-s (get-channel "buyer1" "seller" infra)
         s-b1 (get-channel "seller" "buyer1" infra)
         b1-b2 (get-channel "buyer1" "buyer2" infra)]
     (buyer1 b1-s s-b1 b1-b2 1)))
  ([b1-s s-b1 b1-b2 iteration]
   (println iteration)
   (>!!! b1-s (msg "title" "book"))
   (do (<!!!! s-b1 "quote")
       (>!!! b1-b2 (msg "quote-div" 16))
       (when (<!!!! s-b1 "date")
         (buyer1 b1-s s-b1 b1-b2 (+ iteration 1))))))

(defn buyer2 "Order a book from buyer2's perspective"
  ([infra]
   (let [s-b2 (get-channel "seller" "buyer2" infra)
         b1-b2 (get-channel "buyer1" "buyer2" infra)
         b2-s (get-channel "buyer2" "seller" infra)]
     (buyer2 s-b2 b1-b2 b2-s)))
  ([s-b2 b1-b2 b2-s]
   (do (<!!!! s-b2 "quote")
       (<!!!! b1-b2 "quote-div")
       (>!!! b2-s (msg "ok" "ok"))
       (>!!! b2-s (msg "address" "address"))
       (<!!! s-b2 "date")
       (buyer2 s-b2 b1-b2 b2-s))))

(defn seller "Order book from seller's perspective"
  ([infra]
   (let [b1-s (get-channel "buyer1" "seller" infra)
         s-b1 (get-channel "seller" "buyer1" infra)
         s-b2 (get-channel "seller" "buyer2" infra)
         b2-s (get-channel "buyer2" "seller" infra)]
     (seller b1-s s-b1 s-b2 b2-s)))
  ([b1-s s-b1 s-b2 b2-s]
   (do (<!!!! b1-s "title")
       (>!!! [s-b1 s-b2] (msg "quote" 20))
       (<!!!! b2-s "ok")
       (<!!!! b2-s "address")
       (>!!! [s-b2 s-b1] (msg "date" 3))
       (seller b1-s s-b1 s-b2 b2-s))))

(def two-buyer-protocol
  (mep
    (rec :order-book
         (-->> "title" "buyer1" "seller")
         (-->> "quote" "seller" ["buyer1" "buyer2"])
         (-->> "quote-div" "buyer1" "buyer2")
         (choice
           [(-->> "ok" "buyer2" "seller")
            (-->> "address" "buyer2" "seller")
            (-->> "date" "seller" ["buyer1""buyer2"])
            (continue :order-book)]
           [(-->> "quit" "buyer2" "seller")]))))

;generate the infra structure for the protocol
(def infrastructure (add-infrastructure two-buyer-protocol))
(set-logging-exceptions)
;start each participant on another thread
(defn start! []
  (do (thread (buyer1 infrastructure))
      (thread (buyer2 infrastructure))
      (thread (seller infrastructure))))
(start!)

