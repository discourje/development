(ns discourje.benchmarks.OneBuyer
  (:require [discourje.core.async :refer :all]
            [discourje.core.logging :refer :all])
  (:use [slingshot.slingshot :only [throw+ try+]]))

(def buy-goods
  (mep
    (-->> "quote-request" "buyer" "seller")
    (choice
      [(-->> "quote" "seller" "buyer")
       (-->> "order" "buyer" "seller")
       (-->> "order-ack" "seller" "buyer")]
      [(-->> "out-of-stock" "seller" "buyer")])))

(defn discourje-buyer
  "Logic representing Buyer"
  [b->s s->b quote-request order]
  (>!! b->s quote-request)
  (if (= (get-label (<!! s->b ["quote" "out-of-stock"])) "quote")
    (do (>!! b->s order)
        (<!! s->b "order-ack"))
    "Book is out of stock!"))

(defn discourje-seller
  "Logic representing the Seller"
  [b->s s->b in-stock? quote order-ack out-of-stock]
  (if (== 1 (in-stock? (get-content (<!! b->s "quote-request"))))
    (do
      (>!! s->b quote)
      (<!! b->s "order")
      (>!! s->b order-ack))
    (>!! s->b out-of-stock)))

(defn discourje-one-buyer []
  (let [infra (add-infrastructure buy-goods)
        b->s (get-channel "buyer" "seller" infra)
        s->b (get-channel "seller" "buyer" infra)
        product {:product-type "book" :content {:title "The joy of Clojure"}}
        quote-request (msg "quote-request" product)
        order (msg "order" "confirm order!")
        in-stock? (fn [book] (rand-int 2))
        quote (msg "quote" "$40,00")
        order-ack (msg "order-ack" "order-ack confirmed!")
        out-of-stock (msg "out-of-stock" "Product out of stock!")]
    (time
      (do
        (clojure.core.async/thread (discourje-buyer b->s s->b quote-request order))
        (discourje-seller b->s s->b in-stock? quote order-ack out-of-stock)))
    (clojure.core.async/close! (get-chan b->s))
    (clojure.core.async/close! (get-chan s->b))))
(set-logging-exceptions)
(discourje-one-buyer)

(defn clojure-buyer
  "Logic representing Buyer"
  [b->s s->b quote-request order]
  (do (clojure.core.async/>!! b->s quote-request)
      (if (= (get-label (clojure.core.async/<!! s->b)) "quote")
        (do (clojure.core.async/>!! b->s order)
            (clojure.core.async/<!! s->b))
        "Book is out of stock!")))

(defn clojure-seller
  "Logic representing the Seller"
  [b->s s->b in-stock? quote order-ack out-of-stock]
  (if (== 1 (in-stock? (clojure.core.async/<!! b->s)))
    (do
      (clojure.core.async/>!! s->b quote)
      (clojure.core.async/<!! b->s)
      (clojure.core.async/>!! s->b order-ack))
    (clojure.core.async/>!! s->b out-of-stock)))

(defn clojure-one-buyer []
  (let [b->s (clojure.core.async/chan 1)
        s->b (clojure.core.async/chan 1)
        product {:product-type "book" :content {:title "The joy of Clojure"}}
        quote-request (msg "quote-request" product)
        order (msg "order" "confirm order!")
        in-stock? (fn [book] (rand-int 2))
        quote (msg "quote" "$40,00")
        order-ack (msg "order-ack" "order-ack confirmed!")
        out-of-stock (msg "out-of-stock" "Product out of stock!")]
    (time
      (do
        (clojure.core.async/thread (clojure-buyer b->s s->b quote-request order))
        (clojure-seller b->s s->b in-stock? quote order-ack out-of-stock)))
    (clojure.core.async/close! b->s)
    (clojure.core.async/close! s->b)))

(clojure-one-buyer)