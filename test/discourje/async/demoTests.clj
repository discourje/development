(ns discourje.async.demoTests
  (:require [clojure.test :refer :all]
            [discourje.core.async :refer :all])
  (:import (clojure.lang PersistentArrayMap)))

(def buy-goods
  (mep
    (-->> PersistentArrayMap "buyer" "seller")
    (choice
      [(-->> (fn [v] (= (:choice v) "quote")) "seller" "buyer")
       (-->> String "buyer" "seller")
       (-->> String "seller" "buyer")]
      [(-->> (fn [v] (= (:choice v) "out-of-stock")) "seller" "buyer")])))

(def buyer-to-seller (chan "buyer" "seller" 1))
(def seller-to-buyer (chan "seller" "buyer" 2))
(def custom-infra
  (add-infrastructure buy-goods
                      [buyer-to-seller seller-to-buyer]))
(defn buyer
  "Logic representing Buyer"
  [infra confirmed-callback out-of-stock-callback]
  (let [b->s (get-channel infra "buyer" "seller")
        s->b (get-channel infra "seller" "buyer")
        product {:product-type "book" :content {:title "The joy of Clojure"}}]
    (>!! b->s product)
    (if (= (:choice (<!! s->b) "quote"))
      (do (>!! b->s "confirm order!")
          (confirmed-callback (<!! s->b)))
      (out-of-stock-callback "Book is out of stock!"))))

(defn seller
  "Logic representing the Seller"
  [infra]
  (let [b->s (get-channel infra "buyer" "seller")
        s->b (get-channel infra "seller" "buyer")
        in-stock? (fn [book] (rand-int 2))]
    (if (== 1 (in-stock? (<!! b->s)))
      (do
        (>!! s->b {:choice "quote" :content "$40,00"})
        (let [order (<!! b->s)]
          (println order)
          (>!! s->b "order-ack confirmed!")))
      (>!! s->b {:choice "out-of-stock" :content "Product out of stock!"}))))

(deftest demo-test
  (let [infra (add-infrastructure buy-goods)
        buyer (clojure.core.async/thread (buyer infra (fn [m] (println m) m) (fn [m] (println m) m)))]
    (clojure.core.async/thread (seller infra))
    (is (or
          (= buyer "Book is out of stock!")
          (= buyer "order-ack confirmed!")))))

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