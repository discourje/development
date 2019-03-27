(ns discourje.demo.step2
  (:require [discourje.core.async :refer :all]
            [clojure.core.async :refer [thread]])
  (:import (discourje.demo.javaObjects Book Quote Order QuoteRequest OutOfStock OrderAcknowledgement)))

;First Step is to change the namespace.

;And define a MEP for the buy-goods protocol
(def buy-goods
  (mep
    (-->> QuoteRequest "buyer" "seller")
    (choice
      [(-->> Quote "seller" "buyer")
       (-->> Order "buyer" "seller")
       (-->> OrderAcknowledgement "seller" "buyer")]
      [(-->> OutOfStock "seller" "buyer")])))

;Second step is to add infra structure to our MEP

;define channels
;(def buyer-to-seller (chan))
;(def seller-to-buyer (chan))

(def infra (add-infrastructure buy-goods))
(def buyer-to-seller (get-channel "buyer" "seller" infra))
(def seller-to-buyer (get-channel "seller" "buyer" infra))

(def product (doto (Book.) (.setName "The Joy of Clojure")))

(defn in-stock? "return a 50% change true in stock" [book]
  (let [in-stock (== 1 (rand-int 2))]
    (println (format "%s is in stock: %s" (.getName book) in-stock))
    in-stock))

;define buyer logic
(defn buyer "Logic representing Buyer" []
  (>!! buyer-to-seller product)
  (let [quote (<!! seller-to-buyer)]
    (if (.isInStock quote)
      (do (>!! buyer-to-seller (doto (Order.) (.setProduct product) (.setQuote quote)))
          (println (<!! seller-to-buyer)))
      (println "Book is out of stock!"))))

;define seller
(defn seller "Logic representing the Seller" []
  (if (in-stock? (<!! buyer-to-seller))
    (do (>!! seller-to-buyer (doto (Quote.) (.setInStock true) (.setPrice 40.00) (.setProduct product)))
        (let [order (<!! buyer-to-seller)]
          (>!! seller-to-buyer (format "order-acknowledgement: Order for product: %s confirmed at price: $%s"
                                       (.getName (.getProduct order)) (.getPrice (.getQuote order))))))
    (>!! seller-to-buyer (doto (Quote.) (.setInStock false) (.setPrice 0) (.setProduct product)))))

(thread (buyer))
(thread (seller))