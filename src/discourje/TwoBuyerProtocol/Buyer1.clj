(ns discourje.TwoBuyerProtocol.Buyer1
  (:require [discourje.core.core :refer :all]
            [discourje.core.dataStructures :refer :all]))

(defn generateBook
  "generate simple book title"
  []
  "TheJoyOfClojure")

(defn quoteDiv
  "returns true when the received quote is <= 15 (50% chance at this moment)"
  [quote]
  (println (format "received quote: %s" quote))
  (let [randomN (+ (rand-int quote) 1)]
    (println "QD = " randomN)
    randomN))

(defn orderBook
  "order a book from buyer1's perspective (implements new receive monitor)"
  [participant]
  (send-to participant "title" (generateBook) "seller")
  (receive-by participant "quote" "seller"
              (fn [x]
                (println "buyer1 received quote!")
                  (send-to participant "quoteDiv" (quoteDiv x) "buyer2")))
  (receive-by participant "repeat" "buyer2"
              (fn [repeat](println "repeat received on buyer1 from buyer2!")
                  (orderBook participant))
              )
  )

;send title to seller
;wait for quote
;send quote div to buyer2