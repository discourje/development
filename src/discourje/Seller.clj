(ns discourje.Seller
  (:require [discourje.core.core :refer :all])
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

(defn- endReached [quit]
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
                        ;(Thread/sleep 2000) ;quick fix for multiple sends...
                        ;(send! "repeat" "repeat" this ["buyer2" "buyer1"] protocol)
                        ;(orderBook this protocol)
                        ))
             (= response "quit")
             (endReached response)
             )
           )))

;(let [response (atom nil)]
;  (recv! "contribute" "buyer2" "seller" protocol (fn [x] (reset! response x)))
;  (cond
;    (= @response "ok") (println "received Ok!")
;    (= @response "quit") (println "received quit!")
;    (= @response nil) (println "still nil"))
;  )

;wait for title
;send quote to buyer1 and buyer2
;wait for ok or quit
;ok
;wait for address
;send date to buyer2
;quit
;quit
;quit protocol!
