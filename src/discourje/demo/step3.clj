(ns discourje.demo.step3
  (:require [discourje.core.async :refer :all]
            [discourje.core.logging :refer :all])
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

(def infra (add-infrastructure buy-goods))
(def buyer-to-seller (get-channel infra "buyer" "seller"))
(def seller-to-buyer (get-channel infra "seller" "buyer"))

(def product (doto (Book.) (.setName "The Joy of Clojure")))

(defn in-stock? "return a 50% chance true in stock" [book]
  (let [in-stock (== 1 (rand-int 2))]
    (println (format "%s is in stock: %s" (.getName (.getProduct book)) in-stock))
    in-stock))

;Third step is to use Discourje put and take abstractions
;So why were only the take functions not compiling?
;Well, Discourje needs some way to identify communication.
;As we can see, the MEP labels communication between the participants e.g. QuoteRequest Buyer -> Seller.
;So when we take form a channel we need to identify this take operation with the label to verify.
;In the case of this demo, we do not specifically need to identify our send operations since we specified our message labels as Java types.
;When putting on a channel, Discourje will verify if the data being send is a message (msg[:label :content]) data structure.
;If not it will generate a new message with the incoming data type as label.

;To demonstrate what happens if invalid communication is detected we can uncomment line: 45
;On this line there is an unspecified communication from seller to buyer.
;Here we send a new message labelled `miscommunication' with content `Diverging from MEP'.
;When we run the code now, Discourje will throw an exception and a programmer/ system can act accordingly.
;Note that throwing exceptions is blocking, if we do not want communication to block when invalid, we can enable logging instead of throwing exceptions.

;define buyer logic
(defn buyer "Logic representing Buyer" []
  (>!! buyer-to-seller (doto (QuoteRequest.) (.setProduct product)))
  (let [quote (<!! seller-to-buyer)]
    (do (if (.isInStock quote)
          (do (>!! buyer-to-seller (doto (Order.) (.setProduct product) (.setQuote quote)))
              (println (.getMessage (<!! seller-to-buyer))))
          (println "Book is out of stock!"))
        (close! buyer-to-seller)
        (close! seller-to-buyer))))

;define seller
(defn seller "Logic representing the Seller" []
  ;(>!! seller-to-buyer (msg "miscommunication" "Diverging from MEP")) ;Uncomment to introduce miscommunication
  (if (in-stock? (<!! buyer-to-seller))
    (do (>!! seller-to-buyer (doto (Quote.) (.setInStock true) (.setPrice 40.00) (.setProduct product)))
        (let [order (<!! buyer-to-seller)]
          (>!! seller-to-buyer
               (doto (OrderAcknowledgement.)
                 (.setOrder order)
                 (.setMessage
                   (format "order-acknowledgement: Order for product: %s confirmed at price: $%s"
                           (.getName (.getProduct order)) (.getPrice (.getQuote order))))))))
    (>!! seller-to-buyer (doto (Quote.) (.setInStock false) (.setPrice 0) (.setProduct product)))))

(set-logging-exceptions)
;(set-logging)

(thread (buyer))
(thread (seller))
