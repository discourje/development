(ns discourje.TwoBuyerProtocol.Buyer2
  (:require [discourje.core.async :refer :all]
            [discourje.core.logging :refer :all]))

(defn contribute? "returns true when the received quote 50% or greater" [quote div]
  (log-message (format "received quote: %d and div: %d, contribute = %s" quote div (>= (* 100 (float (/ div quote))) 50)))
  (>= (* 100 (float (/ div quote))) 50))

(defn generate-address "generates the address" []
  "Open University, Valkenburgerweg 177, 6419 AT, Heerlen")

(defn order-book "Order a book from buyer2's perspective" [infra]
  (let [s-b2 (get-channel infra"seller" "buyer2")
        b1-b2 (get-channel infra "buyer1" "buyer2")
        b2-s (get-channel infra "buyer2" "seller")
        quote  (<!!! s-b2 "quote")
        quote-div  (<!! b1-b2 "quote-div")]
    (if (contribute? quote quote-div)
      (do (>!! b2-s (msg "ok" {:choice "ok"}))
          (>!! b2-s (msg "address" (generate-address)))
          (let [date (<!!! s-b2 "date")]
            (log-message (format "Thank you, I will put %s in my agenda!"  date))
            (order-book infra)))
      (>!! b2-s (msg "quit" {:choice "quit"})))))

;wait for quote
;wait for quote div
;branchable on data
;true
;send ok to seller
;send address to seller
;wait for date
;false
;quit