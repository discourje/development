(ns discourje.multi.Buyer2
  (:require [discourje.multi.core :refer :all]))


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
  "Order a book from buyer2's perspective"
  [protocol]
  (let [quote (atom nil)
        quoteDiv (atom nil)]
    (recv! "quote" "seller" "buyer2" protocol (fn [receivedQuote] (reset! quote receivedQuote)))
    (recv! "quoteDiv" "buyer1" "buyer2" protocol (fn [receivedQuoteDiv] (reset! quoteDiv receivedQuoteDiv)))
    (add-watch quoteDiv nil
               (fn [key atom old-state new-state]
                 (println (format "quote and quoteDiv are %s %s respectively" @quote new-state))
                 ;(send! "quoteDiv" (quoteDiv new-state) "buyer1" "buyer2" protocol)
                 (remove-watch quoteDiv nil)))))


;wait for quote
;wait for quote div
;branch on data
  ;true
    ;send ok to seller
    ;send address to seller
    ;wait for date
  ;false
    ;quit