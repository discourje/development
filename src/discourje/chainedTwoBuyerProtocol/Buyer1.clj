(ns discourje.chainedTwoBuyerProtocol.Buyer1
  (:require [discourje.api.api :refer :all]))

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
  (s!!> "title" (generateBook) participant "seller"
        (r! "quote" "seller" participant
             (>s!!> "quoteDiv" quoteDiv participant "buyer2"
                    (r! "repeat" "buyer2" participant
                         (fn [repeat] (log "repeat received on buyer1 from buyer2!")
                           (orderBook participant)))))))

(clojure.walk/macroexpand-all `(s!!> "title" (generateBook) "b1" "seller"
                                     (r! "quote" "seller" "b1"
                                          (>s!!> "quoteDiv" quoteDiv "b1" "buyer2"
                                                 (r! "repeat" "buyer2" "b1"
                                                      (fn [repeat] (log "repeat received on buyer1 from buyer2!")
                                                        (orderBook "b1")))))))


;send title to seller
;wait for quote
;send quote div to buyer2