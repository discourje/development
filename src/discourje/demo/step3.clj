(ns discourje.demo.step3
  (:require [discourje.core.async :refer :all]
            [discourje.core.logging :refer :all])
  (:import (discourje.demo.javaObjects Book Quote Order)))

;First Step is to change the namespace.

;And define a MEP for the buy-goods protocol
(def buy-goods
  (mep
    (-->> "quote-request" "buyer" "seller")
    (choice
      [(-->> Quote "seller" "buyer")
       (-->> Order "buyer" "seller")
       (-->> "order-acknowledgement" "seller" "buyer")]
      [(-->> Quote "seller" "buyer")])))

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

;Third step is to use Discourje put and take abstractions

;Data being send through Discourje abstractions are of type:
;message: msg[:label :content]

;define buyer logic
(defn buyer "Logic representing Buyer" []
  (>!! buyer-to-seller (msg "quote-request" product))
  (let [quote (get-content (<!! seller-to-buyer Quote))]
        (if (.isInStock quote)
          (do (>!! buyer-to-seller (doto (Order.) (.setProduct product) (.setQuote quote)))
              (println (get-content (<!! seller-to-buyer "order-acknowledgement"))))
          (println "Book is out of stock!"))))

;define seller
(defn seller "Logic representing the Seller" []
  ;(>!! s->b (msg "miscommunication" "Diverging from MEP")) ;Uncomment to introduce miscommunication
  (if (in-stock? (get-content (<!! buyer-to-seller "quote-request")))
    (do (>!! seller-to-buyer (doto (Quote.) (.setInStock true) (.setPrice 40.00) (.setProduct product)))
        (let [order (get-content (<!! buyer-to-seller Order))]
          (>!! seller-to-buyer (msg "order-acknowledgement"
                                    (format "order-acknowledgement: Order for product: %s confirmed at price: $%s"
                                            (.getName (.getProduct order)) (.getPrice (.getQuote order)))))))
    (>!! seller-to-buyer (doto (Quote.) (.setInStock false) (.setPrice 0) (.setProduct product)))))

;(set-logging-and-exceptions)
(set-logging-exceptions)
;(set-logging)

(clojure.core.async/thread (buyer))
(clojure.core.async/thread (seller))