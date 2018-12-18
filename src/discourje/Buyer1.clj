(ns discourje.Buyer1
  (:require [discourje.core.core :refer :all]))

(defn generateBook
  "generate simple book title"
  []
  "TheJoyOfClojure")

(defn quoteDiv
  "returns true when the received quote is <= 15 (50% chance at this moment)"
  [quote]
  (println (format "received quote: %s" quote))
  (+ (rand-int quote) 1))

;(defn orderBook
;  "order a book from buyer1's perspective (implements new receive monitor)"
;  [protocol]
;  (send! "title" (generateBook) "buyer1" "seller" protocol)
;  (let [quote (atom nil)]
;    (recv! "quote" "seller" "buyer1" protocol (fn [receivedQuote] (reset! quote receivedQuote)))
;    (add-watch quote nil
;               (fn [key atom old-state new-state]
;                 (send! "quoteDiv" (quoteDiv new-state) "buyer1" "buyer2" protocol)
;                 (remove-watch quote nil)))
;  ))

(defn orderBook
  "order a book from buyer1's perspective (implements new receive monitor)"
  [this protocol]
  (send! "title" (generateBook) this "seller" protocol)
  (recv! "quote" "seller" this protocol
         (fn [x]
           (send! "quoteDiv" (quoteDiv x) this "buyer2" protocol))))


;(clojure.core.async/thread (orderBook))

;send title to seller
;wait for quote
;send quote div to buyer2