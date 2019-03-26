(ns discourje.demo.step0
  (:require [clojure.core.async :refer [>!! <!! chan thread]])
  (:import (discourje.demo.javaObjects Book)))

;Standard Clojure.core.async code


;define channels
(def buyer-to-seller (chan))
(def seller-to-buyer (chan))

(def product (doto (Book.) (.setName "The Joy of Clojure")))

;define buyer logic
(defn buyer "Logic representing Buyer" []
  (>!! buyer-to-seller product)
  (if (not= (<!! seller-to-buyer) "out-of-stock")
    (do (>!! buyer-to-seller "confirm order!")
        (println (<!! seller-to-buyer)))
    (println "Book is out of stock!")))

;define seller
(defn seller "Logic representing the Seller" []
  (let [in-stock? (fn [book] (let [in-stock (== 1(rand-int 2))](println (format "%s is in stock: %s" (.getName book) in-stock)) in-stock))]
    (if (in-stock? (<!! buyer-to-seller))
      (do (>!! seller-to-buyer "$40,00")
          (let [order (<!! buyer-to-seller)]
            (println order)
            (>!! seller-to-buyer "order-acknowledgement!")))
      (>!! seller-to-buyer "out-of-stock"))))

(thread (buyer))
(thread (seller))
