(ns discourje.TwoBuyerProtocol.Buyer2
  (:require [discourje.api.api :refer :all]))

(defn contribute?
  "returns true when the received quote 50% or greater"
  [quote div]
  (println (format "received quote: %d and div: %d, contribute = %s" quote div (>= (* 100 (float (/ div quote))) 50)))
  (>= (* 100 (float (/ div quote))) 0)) ;todo set value to 50% when done debugging!

(defn generateAddress
  "generates the address"
  []
  "Open University, Valkenburgerweg 177, 6419 AT, Heerlen")

(defn orderBook
  "Order a book from buyer2's perspective"
  [participant]
  (r! "quote" "seller" participant
              (fn [receivedQuote]
                  (println "buyer2 received quote! " receivedQuote)
                  (r! "quoteDiv" "buyer1" participant
                              (fn [receivedQuoteDiv]
                                  (if (contribute? receivedQuote receivedQuoteDiv)
                                    (do (s! "ok" "ok" participant "seller")
                                        (s! "address" (generateAddress) participant  "seller")
                                        (r! "date" "seller" participant
                                                    (fn [x] (println "Received date!" x)
                                                      (s! "repeat" "repeat" participant  ["seller" "buyer1"])
                                                      (orderBook participant))))
                                    (s! "quit" "quit" participant "seller")))))))

;wait for quote
;wait for quote div
;branch on data
;true
;send ok to seller
;send address to seller
;wait for date
;false
;quit