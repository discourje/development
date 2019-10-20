(ns discourje.benchmarks.OneBuyer
  (:require [discourje.core.async :refer :all]
            [discourje.core.logging :refer :all])
  (:import (clojure.lang PersistentArrayMap)))

(def buy-goods
  (mep
    (-->> PersistentArrayMap "buyer" "seller")
    (choice
      [(-->> String "seller" "buyer")
       (-->> String "buyer" "seller")
       (-->> String "seller" "buyer")]
      [(-->> String "seller" "buyer")])
    )
  )

(defn- discourje-buyer
  "Logic representing Buyer"
  [b->s s->b quote-request order]
  (>!! b->s quote-request)
  (if (= (<!! s->b) "$40,00")
    (do (>!! b->s order)
        (<!! s->b))
    "Book is out of stock!"))

(defn- discourje-seller
  "Logic representing the Seller"
  [b->s s->b in-stock? quote order-ack out-of-stock]
  (if (== 1 (in-stock? (<!! b->s)))
    (do
      (>!! s->b quote)
      (<!! b->s)
      (>!! s->b order-ack))
    (>!! s->b out-of-stock)))

(defn discourje-one-buyer [iterations]
  (let [infra (vec (for [_ (range iterations)] (add-infrastructure buy-goods)))
        b->s (vec (for [i infra] (get-channel i "buyer" "seller")))
        s->b (vec (for [i infra] (get-channel i "seller" "buyer")))
        product {:product-type "book" :content {:title "The joy of Clojure"}}
        quote-request "quote-request"
        order "order"
        in-stock? (fn [book] (rand-int 2))
        quote "$40,00"
        order-ack "order-ack confirmed!"
        out-of-stock "Product out of stock!"
        time (custom-time
               (doseq [i (range iterations)]
                 (do
                   (clojure.core.async/thread (discourje-seller (nth b->s i) (nth s->b i) in-stock? quote order-ack out-of-stock))
                   (discourje-buyer (nth b->s i) (nth s->b i) quote-request order))))]
    (doseq [i (range iterations)]
      (clojure.core.async/close! (get-chan (nth b->s i)))
      (clojure.core.async/close! (get-chan (nth s->b i))))
    time))

(defn discourje-one-buyer-monitor-reset [iterations]
  (let [infra (add-infrastructure buy-goods)
        b->s (get-channel infra "buyer" "seller")
        s->b (get-channel infra "seller" "buyer")
        interactions (get-active-interaction (get-monitor b->s))
        product {:product-type "book" :content {:title "The joy of Clojure"}}
        quote-request product
        order "confirm order!"
        in-stock? (fn [book] (rand-int 2))
        quote "$40,00"
        order-ack "order-ack confirmed!"
        out-of-stock "Product out of stock!"
        time (custom-time
               (doseq [_ (range iterations)]
                 (do
                   (clojure.core.async/thread (discourje-seller b->s s->b in-stock? quote order-ack out-of-stock))
                   (discourje-buyer b->s s->b quote-request order)
                   (force-monitor-reset! (get-monitor b->s) interactions))))]

    time))

(defn- clojure-buyer
  "Logic representing Buyer"
  [b->s s->b quote-request order]
  (do (clojure.core.async/>!! b->s quote-request)
      (if (= (clojure.core.async/<!! s->b) "$40,00")
        (do (clojure.core.async/>!! b->s order)
            (clojure.core.async/<!! s->b))
        "Book is out of stock!")))

(defn- clojure-seller
  "Logic representing the Seller"
  [b->s s->b in-stock? quote order-ack out-of-stock]
  (if (== 1 (in-stock? (clojure.core.async/<!! b->s)))
    (do
      (clojure.core.async/>!! s->b quote)
      (clojure.core.async/<!! b->s)
      (clojure.core.async/>!! s->b order-ack))
    (clojure.core.async/>!! s->b out-of-stock)))

(defn clojure-one-buyer [iterations]
  (let [b->s (vec (for [_ (range iterations)] (clojure.core.async/chan 1)))
        s->b (vec (for [_ (range iterations)] (clojure.core.async/chan 1)))
        product {:product-type "book" :content {:title "The joy of Clojure"}}
        quote-request product
        order "confirm order!"
        in-stock? (fn [book] (rand-int 2))
        quote  "$40,00"
        order-ack "order-ack confirmed!"
        out-of-stock "Product out of stock!"
        time (custom-time
               (doseq [i (range iterations)]
                 (do
                   (clojure.core.async/thread (clojure-buyer (nth b->s i) (nth s->b i) quote-request order))
                   (clojure-seller (nth b->s i) (nth s->b i) in-stock? quote order-ack out-of-stock))))]
    (doseq [i (range iterations)] (clojure.core.async/close! (nth b->s i))
                                  (clojure.core.async/close! (nth s->b i)))
    time))

(defn clojure-one-buyer-reset [iterations]
  (let [infra (add-infrastructure buy-goods)
        b->s (get-channel infra "buyer" "seller")
        s->b (get-channel infra "seller" "buyer")
        interactions (get-active-interaction (get-monitor b->s))
        product {:product-type "book" :content {:title "The joy of Clojure"}}
        quote-request product
        order "confirm order!"
        in-stock? (fn [book] (rand-int 2))
        quote  "$40,00"
        order-ack "order-ack confirmed!"
        out-of-stock  "Product out of stock!"
        time (custom-time
               (doseq [_ (range iterations)]
                 (do
                   (clojure.core.async/thread (clojure-seller (get-chan b->s) (get-chan s->b) in-stock? quote order-ack out-of-stock))
                   (clojure-buyer (get-chan b->s) (get-chan s->b) quote-request order)
                   (force-monitor-reset! (get-monitor b->s) interactions))))]
    time))
;(clojure-one-buyer 1)
;(clojure-one-buyer 2)
;(clojure-one-buyer 4)
;(clojure-one-buyer 8)
;(clojure-one-buyer 16)
;(clojure-one-buyer 32)
;(clojure-one-buyer 64)
;(clojure-one-buyer 128)
;(clojure-one-buyer 256)