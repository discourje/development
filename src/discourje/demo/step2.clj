(ns discourje.demo.step2
  (:require [discourje.core.async :refer :all])
  (:import (discourje.demo.javaObjects Book Quote Order QuoteRequest OutOfStock OrderAcknowledgement)))

(def buy-goods
  (mep
    (-->> QuoteRequest "buyer" "seller")
    (choice
      [(-->> Quote "seller" "buyer")
       (-->> Order "buyer" "seller")
       (-->> OrderAcknowledgement "seller" "buyer")]
      [(-->> OutOfStock "seller" "buyer")])
    (close "buyer" "seller")
    (close "seller" "buyer")))

;Second step is to add infra structure to our MEP
(def infra (add-infrastructure buy-goods))
;So instead of defining our channels we allow Discourje to generate them
;(def buyer-to-seller (chan))
;(def seller-to-buyer (chan))

;generate the infrastructure
(def infra (add-infrastructure buy-goods))
;query the channels, identified by sender and receiver pairs.
(def buyer-to-seller (get-channel infra "buyer" "seller"))
(def seller-to-buyer (get-channel infra "seller" "buyer"))

(def product (doto (Book.) (.setName "The Joy of Clojure")))

(defn in-stock? "return a 50% change true in stock" [book]
  (let [in-stock (== 1 (rand-int 2))]
    (println (format "%s is in stock: %s" (.getName book) in-stock))
    in-stock))

;The next step is to communicate through Discourje put and take abstractions.
;As we can see, only the take abstraction are not compiling.

;define buyer logic
(defn buyer "Logic representing Buyer" []
  (>!! buyer-to-seller product)
  (let [quote (<!! seller-to-buyer1)]
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
