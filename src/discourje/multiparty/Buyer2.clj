(ns discourje.multiparty.Buyer2
  (:require [discourje.multiparty.core :refer :all]))


(defn contribute?
  "returns true when the received quote is <= 15 (50% chance at this moment)"
  [quote]
  (println (format "Only contribute up to 15, quote is %s" quote))
  (<= quote 15))

(defn generateAddress
  "generates the address"
  []
  "Open University, Valkenburgerweg 177, 6419 AT, Heerlen")



;wait for quote
;wait for quote div
;branch on data
  ;true
    ;send ok to seller
    ;send address to seller
    ;wait for date
  ;false
    ;quit