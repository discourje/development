(ns discourje.multi.Buyer1
  (:require [discourje.multi.core :refer :all]))


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
  "order a book from buyer1's perspective (implements new receive monitor)"
  [protocol]
  (send! "title" (generateBook) "buyer1" "seller" protocol)
  (let [quote (recv! "quote" "seller" "buyer1" protocol)]
    (send! "quoteDiv" (quoteDiv quote) "buyer1" "buyer2" protocol)))
;(clojure.core.async/thread (orderBook))

;send title to seller
;wait for quote
;send quote div to buyer2