(ns discourje.multi.Buyer2
  (:require [discourje.multi.core :refer :all]))

(defn contribute?
  "returns true when the received quote 50% or greater"
  [quote div]
  (println (format "received quote: %d and div: %d, contribute = %s" quote div (>= (* 100 (float (/ div quote))) 50)))
  (>= (* 100 (float (/ div quote))) 50))

(defn generateAddress
  "generates the address"
  []
  "Open University, Valkenburgerweg 177, 6419 AT, Heerlen")

;(defn orderBook
;  "Order a book from buyer2's perspective"
;  [protocol]
;  (let [quote (atom nil)
;        quoteDiv (atom nil)]
;    (recv! "quote" "seller" "buyer2" protocol (fn [receivedQuote] (reset! quote receivedQuote)))
;    (recv! "quoteDiv" "buyer1" "buyer2" protocol (fn [receivedQuoteDiv] (reset! quoteDiv receivedQuoteDiv)))
;    (add-watch quoteDiv nil
;               (fn [key atom old-state new-state]
;                 (println (format "quote and quoteDiv are %s %s respectively" @quote new-state))
;                 (if (contribute? @quote new-state)
;                   (send! "ok" "ok" "buyer2" "seller" protocol)
;                   (send! "quit" "quit" "buyer2" "seller" protocol)
;                   )
;                 (remove-watch quoteDiv nil)))))

(defn orderBook
  "Order a book from buyer2's perspective"
  [this protocol]
  (recv! "quote" "seller" this protocol
         (fn [receivedQuote]
           (recv! "quoteDiv" "buyer1" this protocol
                  (fn [receivedQuoteDiv] (do (println (format "received %s Div %s" receivedQuote receivedQuoteDiv))
                                                      (if (contribute? receivedQuote receivedQuoteDiv)
                                                        (do  (send! "ok" "ok" this "seller" protocol)
                                                             ;(println "waiting")
                                                             ;(println "waiting")
                                                             ;(println "waiting")
                                                             ;(send! "address" (generateAddress) this "seller" protocol)
                                                             (recv! "date" "seller" this protocol (fn [x] (println "Received date!" x))))
                                                        (send! "quit" "quit" this "seller" protocol))
                                                      ))))))

  ;wait for quote
  ;wait for quote div
  ;branch on data
  ;true
  ;send ok to seller
  ;send address to seller
  ;wait for date
  ;false
  ;quit