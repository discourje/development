(ns discourje.TwoBuyerProtocol.Buyer1
  (:require [discourje.core.async :refer :all]
            [discourje.core.logging :refer :all])
  (:use [slingshot.slingshot :only [throw+ try+]]))

(def books ["The Joy of Clojure" "Mastering Clojure Macros" "Programming Clojure"])

(defn generate-book "generate book title" []
  (first (shuffle books)))

(defn quote-div "Generate random number with max, quote value" [quote]
  (log-message (format "received quote: %s" quote))
  (let [randomN (+ (rand-int quote) 1)]
    (log-message "QD = " randomN)
    randomN))

(defn order-book "order a book from buyer1's perspective" [infra]
  (let [b1-s (get-channel infra "buyer1" "seller")
        s-b1 (get-channel infra "seller" "buyer1")
        b1-b2 (get-channel infra "buyer1" "buyer2")]
    (>!! b1-s (msg "title" (generate-book)))
    (let [quote (<!!! s-b1 "quote")
          div (quote-div quote)]
      (do
        (>!! b1-b2 (msg "quote-div" div))
        (when (<!!! s-b1 "date")
          (order-book infra))))))

;send title to seller
;wait for quote
;send quote div to buyer2