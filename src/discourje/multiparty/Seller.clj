(ns discourje.multiparty.Seller
  (:require [discourje.multiparty.core :refer :all])
  (:import (java.util Date Calendar)))

(defn quoteBook
  "generate random integer between 1(inclusive) and 30(inclusive)"
  [title]
  (println (format "received title: %s" title))
  (let [x (+ (rand-int 30) 1)]
    (println (format "random number is: %s" x))
    x))

(defn getDate "Generate a new date and increment an amount of days"
  [days]
  (let [cal (Calendar/getInstance)
        d (new Date)]
    (doto cal
      (.setTime d)
      (.add Calendar/DATE days)
      (.getTime))))

; Generate a new java date with a random amount of days incremented up to a specified range
(defn getRandomDate "Get a random date, in the future, up to a maximum range (inclusive)"
  [maxRange]
  (getDate (+ (rand-int maxRange) 1)))

(defn orderBook
  "order a book from seller's perspective"
  []
  (let [title (discourje.multiparty.TwoBuyersProtocol/communicate "title" "buyer1" "seller")]
    (discourje.multiparty.TwoBuyersProtocol/communicate "quote" (quoteBook title) "seller" "buyer1 AND buyer2")    ;<--- will not work atm!

    ))

;wait for title
;send quote to buyer1 and buyer2
;wait for ok or quit
  ;ok
    ;wait for address
    ;send date to buyer2
  ;quit
    ;quit
;quit protocol!
