(ns discourje.chainedTwoBuyerProtocol.Buyer2
  (:require [discourje.api.api :refer :all]))

(defn contribute?
  "returns true when the received quote 50% or greater"
  [quote div]
  (log (format "received quote: %d and div: %d, contribute = %s" quote div (>= (* 100 (float (/ div quote))) 10)))
  (>= (* 100 (float (/ div quote))) 10))

(defn generateAddress
  "generates the address"
  [x]
  (log "generating address now")
  "Open University, Valkenburgerweg 177, 6419 AT, Heerlen")

(defn orderBook
  "Order a book from buyer2's perspective"
  [participant]
  (r! "quote" "seller" participant
              (fn [receivedQuote]
                  (log "buyer2 received quote! " receivedQuote)
                  (r! "quoteDiv" "buyer1" participant
                              (fn [receivedQuoteDiv]
                                  (if (contribute? receivedQuote receivedQuoteDiv)
                                    (do (s!!> "ok" "ok" participant "seller"
                                              (>!!s!!> "address" generateAddress participant "seller"
                                                       (r! "date" "seller" participant
                                                    (fn [x] (println "Received date!" x)
                                                      (s! "repeat" "repeat" participant  ["seller" "buyer1"])
                                                      (orderBook participant))))))
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

(clojure.walk/macroexpand-all `(s!!> "ok" "ok" participant "seller"
                                     (>!!s!!> "address" generateAddress participant "seller"
                                              (println))))