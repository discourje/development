(ns discourje.TwoBuyerProtocolExceptions.Buyer2
  (:require [discourje.core.async :refer :all]
            [discourje.core.logging :refer :all])
  (:use [slingshot.slingshot :only [throw+ try+]]))

(defn contribute? "returns true when the received quote 50% or greater" [quote div]
  (log-message (format "received quote: %d and div: %d, contribute = %s" quote div (>= (* 100 (float (/ div quote))) 50)))
  (>= (* 100 (float (/ div quote))) 50))

(defn generate-address "generates the address" []
  "Open University, Valkenburgerweg 177, 6419 AT, Heerlen")

(defn order-book "Order a book from buyer2's perspective" [infra]
  (let [s-b2 (get-channel "seller" "buyer2" infra)
        b1-b2 (get-channel "buyer1" "buyer2" infra)
        b2-s (get-channel "buyer2" "seller" infra)
        ok-delivered? (atom false)
        quote (get-content (<!!! s-b2 "quote"))
        quote-div (get-content (<!! b1-b2 "quote-div"))]
    (if (contribute? quote quote-div)
      (do
        (>!! b2-s (msg "ok" "ok"))
        (while (false? @ok-delivered?)
          (try+
            (println "sending address")
            (>!! b2-s (msg "address" (generate-address)))
            (reset! ok-delivered? true)
            (catch [:type :incorrect-communication] {}
              (println "address not delivered, retrying..."))))
          (let [date (<!!! s-b2 "date")]
            (log-message (format "Thank you, I will put %s in my agenda!" (get-content date)))
            (order-book infra)))
      (>!! b2-s (msg "quit" "Price to high!")))))

;wait for quote
;wait for quote div
;branchable on data
;true
;send ok to seller
;send address to seller
;wait for date
;false
;quit