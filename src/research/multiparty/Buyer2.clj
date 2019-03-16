(ns research.multiparty.Buyer2
  (:require [research.multiparty.core :refer :all]))


(defn contribute?
  "returns true when the received quote is <= 15 (50% chance at this moment)"
  [quote div]
  (println (format "Only contribute up to 15, quote is %s" quote))
  (<= quote 15))

(defn generateAddress
  "generates the address"
  []
  "Open University, Valkenburgerweg 177, 6419 AT, Heerlen")

(defn orderBook
  "order a book from buyer2's perspective"
  (let [quote (research.multiparty.TwoBuyersProtocol/communicate "quote" "seller" "buyer2")
        quoteDiv (research.multiparty.TwoBuyersProtocol/communicate "quoteDiv" "buyer1" "buyer2")]
    (if (contribute? quote quoteDiv)
      (do (research.multiparty.TwoBuyersProtocol/communicate "ok" "ok" "buyer2" "seller")
          (research.multiparty.TwoBuyersProtocol/communicate "address" (generateAddress) "buyer2" "seller")
          (let [date (research.multiparty.TwoBuyersProtocol/communicate "date" "seller" "buyer2")]
            (println date)))
      (research.multiparty.TwoBuyersProtocol/communicate "quit" "quit" "buyer2" "seller"))))

(orderBook)
;wait for quote
;wait for quote div
;branchable on data
  ;true
    ;send ok to seller
    ;send address to seller
    ;wait for date
  ;false
    ;quit