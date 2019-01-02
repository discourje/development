(ns discourje.TwoBuyerProtocol.Seller
  (:require [discourje.core.core :refer :all]
            [discourje.core.dataStructures :refer :all])
  (:import (java.util Date Calendar)))

(defn quoteBook
  "generate random integer between 1(inclusive) and 30(inclusive)"
  [title]
  (println (format "received title: %s" title))
  (let [x (+ (rand-int 30) 1)]
    (println (format "random number is: %s" x))
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
  [this protocol]
  (recvDelayed! "title" "buyer1" this protocol
                (fn [title]
                  (send! "quote" (quoteBook title) this ["buyer1" "buyer2"] protocol)))
  (recvDelayed! ["ok" "quit"] "buyer2" this protocol
                (fn [response]
                  (cond
                    (= response "ok")
                    (recvDelayed! "address" "buyer2" this protocol
                                  (fn [address]
                                    (println "The received address is: " address)
                                    (send! "date" (getRandomDate 5) this "buyer2" protocol)
                                    (send! "repeat" "repeat" this ["buyer2" "buyer1"] protocol)
                                    (orderBook this protocol)
                                    ))
                    (= response "quit")
                    (endReached response)
                    )
                  )))

(defn orderBookParticipant
  "Order book from seller's perspective"
  ([participant]
  (receive-by participant "title" "buyer1"
              (fn [title] (send-to participant "quote" (quoteBook title) ["buyer1" "buyer2"])))
  (receive-by participant ["ok" "quit"] "buyer2"
              (fn [response]
              (cond
                (= response "ok")
                (receive-by participant "address" "buyer2"
                            (fn [address]
                                (println "The received address is: " address)
                                (send-to participant "date" (getRandomDate 5) "buyer2")
                                (send-to participant "repeat" "repeat" ["buyer2" "buyer1"])
                                (orderBookParticipant participant)
                                ))
                (= response "quit")
                (endReached response)
                )
              )))
  ([name protocol] ; example how function would operate when participants are constructed internally
   (let [participant (discourje.core.core/->participant name protocol)]
     (orderBookParticipant participant)))
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
