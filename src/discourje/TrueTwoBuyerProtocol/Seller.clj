(ns discourje.TrueTwoBuyerProtocol.Seller
  (:require [discourje.core.async :refer :all]
            [discourje.core.logging :refer :all])
  (:import (java.util Date Calendar)))

(defn quote-book "generate random integer between 1(inclusive) and 30(inclusive)" [title]
  (log-message (format "received title: %s" title))
  (+ (rand-int 30) 1))

(defn get-date "Generate a new date and increment an amount of days" [days]
  (let [cal (Calendar/getInstance)
        d (new Date)]
    (doto cal
      (.setTime d)
      (.add Calendar/DATE days)
      (.getTime))))

(defn get-random-date "Get a random date, in the future, up to a maximum range (inclusive)" [maxRange]
  (get-date (+ (rand-int maxRange) 1)))

(defn- end-reached "log protocol end reached" [quit]
  (log-message (format "Protocol ended with: %s" quit)))

(defn order-book "Order book from seller's perspective" [infra]
  (let [b1-s (get-channel "buyer1" "seller" infra)
        s-b1 (get-channel "seller" "buyer1" infra)
        s-b2 (get-channel "seller" "buyer2" infra)
        b2-s (get-channel "buyer2" "seller" infra)
        title (<!! b1-s "title")]
    (>!! [s-b1 s-b2] (msg "quote" (quote-book (get-content title))))
    (let [choice-by-buyer2 (<!! b2-s ["ok" "quit"])]
      (cond
        (= "ok" (get-label choice-by-buyer2))
        (do
          (println (format "Order confirmed, will send to address: %s" (get-content (<!! b2-s "address"))))
          (>!! s-b2 (msg "date" (get-random-date 5)))
          (when (<!! b2-s "repeat")
            (order-book infra)))
        (= "quit" (get-label choice-by-buyer2))
        (end-reached "Quit!")))))

;wait for title
;send quote to buyer1 and buyer2
;wait for ok or quit
;ok
;wait for address
;send date to buyer2
;quit
;quit
;quit protocol!
