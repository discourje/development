(ns discourje.demo.step0
  (:require [clojure.core.async :as async]))

;Standard Clojure.core.async code

;define channel
(def channel (async/chan))

;define buyer logic
(defn buyer
  "Logic representing Buyer"
  []
  (let [product {:product-type "book" :content {:title "The Joy of Clojure"}}]
    (async/>!! channel product))
    (if (not= (async/<!! channel) "out-of-stock")
      (do (async/>!! channel "confirm order!")
          (println (async/<!! channel)))
      (println"Book is out of stock!")))

;define seller
(defn seller
  "Logic representing the Seller"
  []
  (let [in-stock? (fn [book] (rand-int 2))]
    (if (== 1 (in-stock? (async/<!! channel)))
      (do
        (async/>!! channel "$40,00")
        (let [order (async/<!! channel)]
          (println order)
          (async/>!! channel "order-ack confirmed!")))
      (async/>!! channel "out-of-stock"))))

(async/thread (buyer))
(async/thread (seller))
