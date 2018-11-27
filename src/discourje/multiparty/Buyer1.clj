(ns discourje.multiparty.Buyer1
  (:require [discourje.multiparty.core :refer :all]
            ))


(defn generateBook
  "generate simple book title"
  []
  "TheJoyOfClojure")

(defn quoteDiv
  "returns true when the received quote is <= 15 (50% chance at this moment)"
  [quote]
  (println (format "received quote: %s" quote))
  (+ (rand-int quote) 1))

(defn orderBook
  "Order a book from buyer1's perspective"
  []
  (discourje.multiparty.TwoBuyersProtocol/communicate "title" (generateBook) "buyer1" "seller")
  (let [quote (discourje.multiparty.TwoBuyersProtocol/communicate "quote" "seller" "buyer1")]
    (discourje.multiparty.TwoBuyersProtocol/communicate "quoteDiv" (quoteDiv quote) "buyer1" "buyer2")))

(clojure.core.async/thread (orderBook))

;send title to seller
;wait for quote
;send quote div to buyer2