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
  (recvDelayed! "quote" "seller" this protocol
         (fn [x]
           (send! "quoteDiv" (quoteDiv x) this "buyer2" protocol)))
  (recvDelayed! "repeat" "seller" this protocol
                (fn [x](println "repeat received on buyer1 from seller!")
                  (orderBook this protocol))
                )
  )


(defn orderBookParticipant
  "order a book from buyer1's perspective (implements new receive monitor)"
  [participant]
  (send-to participant "title" (generateBook) "seller")
  (receive-by participant "quote" "seller"
              (fn [x]
                  (send-to participant "quoteDiv" (quoteDiv x) "buyer2")))
  (receive-by participant "repeat" "seller"
              (fn [x](println "repeat received on buyer1 from seller!")
                  (orderBookParticipant participant))
              )
  )



;(clojure.core.async/thread (orderBook))

;send title to seller
;wait for quote
;send quote div to buyer2