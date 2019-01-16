(ns discourje.TwoBuyerProtocol.Seller
  (:require [discourje.api.api :refer :all])
  (:import (java.util Date Calendar)))

(defn quoteBook
  "generate random integer between 1(inclusive) and 30(inclusive)"
  [title]
  (log (format "received title: %s" title))
  (let [x (+ (rand-int 30) 1)]
    x))

(defn getDate
  "Generate a new date and increment an amount of days"
  [days]
  (let [cal (Calendar/getInstance)
        d (new Date)]
    (doto cal
      (.setTime d)
      (.add Calendar/DATE days)
      (.getTime))))

(defn getRandomDate "Get a random date, in the future, up to a maximum range (inclusive)"
  [maxRange]
  (getDate (+ (rand-int maxRange) 1)))

(defn- endReached
  "log protocol end reached"
  [quit]
  (log (format "Protocol ended with: %s" quit)))


(defn orderBook
  "Order book from seller's perspective"
  [participant]
   (r! "title" "buyer1" participant
               (fn [title] (s! "quote" (quoteBook title) participant ["buyer1" "buyer2"])))
   (r! ["ok" "quit"] "buyer2" participant
               (fn [response]
                 (cond
                   (= response "ok")
                   (do (log "yes yes received Ok")
                       (r! "address" "buyer2" participant
                                   (fn [address]
                                     (log "The received address is: " address)
                                     (s! "date" (getRandomDate 5) participant "buyer2")
                                     (r! "repeat" "buyer2" participant
                                                 (fn [repeat]
                                                   (log "repeat received on seller from buyer2!")
                                                   (orderBook participant)))
                                     )))
                   (= response "quit")
                   (endReached response)))))
;wait for title
;send quote to buyer1 and buyer2
;wait for ok or quit
;ok
;wait for address
;send date to buyer2
;quit
;quit
;quit protocol!
