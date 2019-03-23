(ns discourje.demo.step3
  (:require [discourje.core.async :refer :all]
            [discourje.core.logging :refer :all]))

;First Step is to change the namespace.

;And define a MEP for the buy-goods protocol
(def buy-goods
  (mep
    (-->> "quote-request" "buyer" "seller")
    (choice
      [(-->> "quote" "seller" "buyer")
       (-->> "order" "buyer" "seller")
       (-->> "order-ack" "seller" "buyer")]
      [(-->> "out-of-stock" "seller" "buyer")])))

;Second step is to add infra structure to our MEP
(def infra (add-infrastructure buy-goods))

;Third step is to use Discourje put and take abstractions

;define buyer logic
(defn buyer
  "Logic representing Buyer"
  []
  (let [b->s (get-channel "buyer" "seller" infra)
        s->b (get-channel "seller" "buyer" infra)
        product {:product-type "book" :content {:title "The joy of Clojure"}}]
    (>!! b->s (msg "quote-request" product))
    (if (= (get-label (<!! s->b ["quote" "out-of-stock"])) "quote")
      (do (>!! b->s (msg "order" "confirm order!"))
          (println (get-content (<!! s->b "order-ack"))))
      (println "Book is out of stock!"))))

;define seller
(defn seller
  "Logic representing the Seller"
  []
  (let [b->s (get-channel "buyer" "seller" infra)
        s->b (get-channel "seller" "buyer" infra)
        in-stock? (fn [book] (rand-int 2))]
   ; (>!! b->s (msg "miscommunication" "Hi buyer, this is seller speaking!")) ;Uncomment to introduce miscommunication
    (if (== 1 (in-stock? (get-content (<!! b->s "quote-request"))))
      (do
        (>!! s->b (msg "quote" "$40,00"))
        (let [order (<!! b->s "order")]
          (println (get-content order))
          (>!! s->b (msg "order-ack" "order-ack confirmed!"))))
      (>!! s->b (msg "out-of-stock" "Product out of stock!")))))

(set-logging-and-exceptions)
;(set-logging-exceptions)

(clojure.core.async/thread (buyer))
(clojure.core.async/thread (seller))