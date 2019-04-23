;This is an example on how standard Clojure.core.async code could be implemented for the buy-goods MEP.
;For this Demo, we used Java Objects as the message types to demonstrate Java Interop.
;Notice that the MEP is hidden in the implementation and you must read and study the code to find out what the message flow is exactly.

;So we start of by having some namespace imports
(ns discourje.demo.step0
  (:require [clojure.core.async :refer [>!! <!! chan thread]])
  (:import (discourje.demo.javaObjects Book Quote Order)))

;Then we define two channels
(def buyer-to-seller (chan))
(def seller-to-buyer (chan))

;Define the product we want to buy, in this case a book called The Joy of Clojure.
(def product (doto (Book.) (.setName "The Joy of Clojure")))

;We define an in-stock function, which simply returns true or false with a 50% chance.
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

;define seller logic
(defn seller "Logic representing the Seller" []
  (if (in-stock? (<!! buyer-to-seller))
    (do (>!! seller-to-buyer (doto (Quote.) (.setInStock true) (.setPrice 40.00) (.setProduct product)))
        (let [order (<!! buyer-to-seller)]
          (>!! seller-to-buyer (format "order-acknowledgement: Order for product: %s confirmed at price: $%s"
                                       (.getName (.getProduct order)) (.getPrice (.getQuote order))))))
    (>!! seller-to-buyer (doto (Quote.) (.setInStock false) (.setPrice 0) (.setProduct product)))))

;run both functions on different threads
(thread (buyer))
(thread (seller))
