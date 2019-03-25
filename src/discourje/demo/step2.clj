(ns discourje.demo.step2
  (:require [discourje.core.async :refer :all]
            [clojure.core.async :refer [thread]]))

;First Step is to change the namespace.

;And define a MEP for the buy-goods protocol
(def buy-goods
  (mep
    (-->> "quote-request" "buyer" "seller")
    (choice
      [(-->> "quote" "seller" "buyer")
       (-->> "order" "buyer" "seller")
       (-->> "order-acknowledgement" "seller" "buyer")]
      [(-->> "out-of-stock" "seller" "buyer")])))

;Second step is to add infra structure to our MEP

;define channels
(def infra (add-infrastructure buy-goods))
(def buyer-to-seller (get-channel "buyer" "seller" infra))
(def seller-to-buyer (get-channel "seller" "buyer" infra))

;define buyer logic
(defn buyer "Logic representing Buyer" []
  (let [product {:product-type "book" :content {:title "The Joy of Clojure"}}]
    (>!! buyer-to-seller product))
  (if (not= (<!! seller-to-buyer) "out-of-stock")
    (do (>!! buyer-to-seller "confirm order!")
        (println (<!! seller-to-buyer)))
    (println"Book is out of stock!")))

;define seller
(defn seller "Logic representing the Seller" []
  (let [in-stock? (fn [book] (rand-int 2))]
    (if (== 1 (in-stock? (<!! buyer-to-seller)))
      (do (>!! seller-to-buyer "$40,00")
          (let [order (<!! buyer-to-seller)]
            (println order)
            (>!! seller-to-buyer "order-acknowledgement!")))
      (>!! seller-to-buyer "out-of-stock"))))

(thread (buyer))
(thread (seller))
