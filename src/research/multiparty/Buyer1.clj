(ns research.multiparty.Buyer1
  (:require [research.multiparty.core :refer :all]))


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
  (research.multiparty.TwoBuyersProtocol/communicate "title" (generateBook) "buyer1" "seller")
  (let [quote (research.multiparty.TwoBuyersProtocol/communicate "quote" "seller" "buyer1")]
    (research.multiparty.TwoBuyersProtocol/communicate "quoteDiv" (quoteDiv quote) "buyer1" "buyer2")))


;(clojure.core.async/thread (orderBook))

;send title to seller
;wait for quote
;send quote div to buyer2