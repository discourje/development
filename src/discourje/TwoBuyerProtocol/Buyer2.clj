(ns discourje.TwoBuyerProtocol.Buyer2
  (:require [discourje.core.core :refer :all]
            [discourje.core.dataStructures :refer :all]))

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
  (receive-by participant "quote" "seller"
              (fn [receivedQuote]
                  (println "buyer2 received quote! " receivedQuote)
                  (receive-by participant "quoteDiv" "buyer1"
                              (fn [receivedQuoteDiv]
                                  (if (contribute? receivedQuote receivedQuoteDiv)
                                    (do (send-to participant "ok" "ok" "seller")
                                        (send-to participant "address" (generateAddress) "seller")
                                        (receive-by participant "date" "seller" (fn [x] (println "Received date!" x)))
                                        (receive-by participant "repeat" "seller"
                                                    (fn [x]
                                                        (println "repeat received on buyer2 from seller!")
                                                        (orderBook participant)))
                                        )
                                    (send-to participant "quit" "quit" "seller"))
                                  )))))

;wait for quote
;wait for quote div
;branch on data
;true
;send ok to seller
;send address to seller
;wait for date
;false
;quit