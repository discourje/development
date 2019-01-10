(ns discourje.TwoBuyerProtocol.Seller
  (:require [discourje.core.core :refer :all]
            [discourje.core.dataStructures :refer :all])
  (:import (java.util Date Calendar)))

(defn quoteBook
  "generate random integer between 1(inclusive) and 30(inclusive)"
  [title]
  (println (format "received title: %s" title))
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
  (println (format "Protocol ended with: %s" quit)))


(defn orderBook
  "Order book from seller's perspective"
  ([participant]
  (receive-by participant "title" "buyer1"
              (fn [title] (send-to participant "quote" (quoteBook title) ["buyer1" "buyer2"])))
  (receive-by participant ["ok" "quit"] "buyer2"
              (fn [response]
              (cond
                (= response "ok")
                (do (println "yes yes received Ok")
                    (receive-by participant "address" "buyer2"
                            (fn [address]
                                (println "The received address is: " address)
                                (send-to participant "date" (getRandomDate 5) "buyer2")
                                (send-to participant "repeat" "repeat" ["buyer2" "buyer1"])
                                (orderBook participant)
                                )))
                (= response "quit")
                (endReached response)
                )
              )))
  ([name protocol] ; example how function would operate when participants are constructed internally
   (let [participant (discourje.core.core/->participant name protocol)]
     (orderBook participant)))
  )
;wait for title
;send quote to buyer1 and buyer2
;wait for ok or quit
;ok
;wait for address
;send date to buyer2
;quit
;quit
;quit protocol!
