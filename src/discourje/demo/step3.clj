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
       (-->> "order-acknowledgement" "seller" "buyer")]
      [(-->> "out-of-stock" "seller" "buyer")])))

;Second step is to add infra structure to our MEP

;define channels
(def infra (add-infrastructure buy-goods))
(def buyer-to-seller (get-channel "buyer" "seller" infra))
(def seller-to-buyer (get-channel "seller" "buyer" infra))

;Third step is to use Discourje put and take abstractions

;Data being send through Discourje abstractions are of type:
;message: msg[:label :content]
(def product {:product-type "book" :content {:title "The joy of Clojure"}})
(defn in-stock? [x] (== 1 (rand-int 2)))

;define buyer logic
(defn buyer "Logic representing Buyer" []
  (>!! buyer-to-seller (msg "quote-request" product))
  (if (= (get-label (<!! seller-to-buyer ["quote" "out-of-stock"])) "quote")
    (do (>!! buyer-to-seller (msg "order" "confirm order!"))
        (println (get-content (<!! seller-to-buyer "order-acknowledgement"))))
    (println "Book is out of stock!")))

;define seller
(defn seller "Logic representing the Seller" []
  ;(>!! s->b (msg "miscommunication" "Diverging from MEP")) ;Uncomment to introduce miscommunication
  (if (in-stock? (get-content (<!! buyer-to-seller "quote-request")))
    (do (>!! seller-to-buyer (msg "quote" "$40,00"))
        (let [order (<!! buyer-to-seller "order")]
          (println (get-content order))
          (>!! seller-to-buyer (msg "order-acknowledgement" "order-ack confirmed!"))))
    (>!! seller-to-buyer (msg "out-of-stock" "Product out of stock!"))))

;(set-logging-and-exceptions)
(set-logging-exceptions)
;(set-logging)

(clojure.core.async/thread (buyer))
(clojure.core.async/thread (seller))