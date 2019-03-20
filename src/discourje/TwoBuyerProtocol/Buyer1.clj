(ns discourje.TwoBuyerProtocol.Buyer1
  (:require [discourje.core.async.async :refer :all]))

(defn generateBook
  "generate simple book title"
  []
  "TheJoyOfClojure")

(defn quoteDiv
  "returns true when the received quote is <= 15 (50% chance at this moment)"
  [quote]
  (log (format "received quote: %s" quote))
  (let [randomN (+ (rand-int quote) 1)]
    (log "QD = " randomN)
    randomN))

(defn orderBook
  "order a book from buyer1's perspective (implements new receive monitor)"
  [participant]
  (s! "title" (generateBook) participant "seller")
  (r! "quote" "seller" participant
              (fn [x]
                (log "buyer1 received quote!")
                  (s! "quoteDiv" (quoteDiv x) participant "buyer2")))
  (r! "repeat" "buyer2" participant
              (fn [repeat](log "repeat received on buyer1 from buyer2!")
                  (orderBook participant))))


;send title to seller
;wait for quote
;send quote div to buyer2