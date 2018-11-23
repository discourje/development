(ns discourje.multiparty.Buyer1
  (:require [discourje.multiparty.core :refer :all]))


(defn generateBook
  "generate simple book title"
  []
  (str "TheJoyOfClojure"))

(defn quoteDiv
  "returns true when the received quote is <= 15 (50% chance at this moment)"
  [quote]
  (println (format "received quote: %s" quote))
  (+ (rand-int quote) 1))


(defn orderBook []
  (send! ""))

;send title to seller
;wait for quote
;send quote div to buyer2