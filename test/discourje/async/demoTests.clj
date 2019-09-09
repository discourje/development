(ns discourje.async.demoTests
  (:require [clojure.test :refer :all]
            [discourje.core.async :refer :all]))

(def buy-goods
  (mep
    (-->> "quote-request" "buyer" "seller")
    (choice
      [(-->> "quote" "seller" "buyer")
       (-->> "order" "buyer" "seller")
       (-->> "order-ack" "seller" "buyer")]
      [(-->> "out-of-stock" "seller" "buyer")])))

(def infra (add-infrastructure buy-goods))

(def buyer-to-seller (chan "buyer" "seller" 1))
(def seller-to-buyer (chan "seller" "buyer" 2))
(def custom-infra
  (add-infrastructure buy-goods
                      [buyer-to-seller seller-to-buyer]))

(defn buyer
  "Logic representing Buyer"
  [confirmed-callback out-of-stock-callback]
  (let [b->s (get-channel infra "buyer" "seller")
        s->b (get-channel infra "seller" "buyer")
        product {:product-type "book" :content {:title "The joy of Clojure"}}]
    (>!! b->s (msg "quote-request" product))
    (if (= (get-label (<!! s->b ["quote" "out-of-stock"])) "quote")
      (do (>!! b->s (msg "order" "confirm order!"))
          (confirmed-callback (<!! s->b "order-ack")))
      (out-of-stock-callback "Book is out of stock!"))))

(defn seller
  "Logic representing the Seller"
  []
  (let [b->s (get-channel infra "buyer" "seller")
        s->b (get-channel infra "seller" "buyer")
        in-stock? (fn [book] (rand-int 2))]
    (if (== 1 (in-stock? (get-content (<!! b->s "quote-request"))))
      (do
        (>!! s->b (msg "quote" "$40,00"))
        (let [order (<!! b->s "order")]
          (println (get-content order))
          (>!! s->b (msg "order-ack" "order-ack confirmed!"))))
      (>!! s->b (msg "out-of-stock" "Product out of stock!")))))

(clojure.core.async/thread (buyer (fn [m] (println (get-content m))) (fn [m] (println m))))
(clojure.core.async/thread (seller))

(def Q&A
  (mep
    (rec :questions?
         (-->> "Questions?" "Ruben" "Public")
         (choice
           [(-->> "Yes" "Public" "Ruben")
            (-->> "Answer" "Ruben" "Public")
            (continue :questions?)]
           [(-->> "No" "Public" "Ruben")])
         (-->> "Thank you for your attention!" "Ruben" "Public"))))