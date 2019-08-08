(ns discourje.benchmarks.OneBuyer
  (:require [discourje.core.async :refer :all]
            [discourje.core.logging :refer :all]))

(def buy-goods
  (mep
    (-->> "quote-request" "buyer" "seller")
    (choice
      [(-->> "quote" "seller" "buyer")
       (-->> "order" "buyer" "seller")
       (-->> "order-ack" "seller" "buyer")]
      [(-->> "out-of-stock" "seller" "buyer")])))

(defn- discourje-buyer
  "Logic representing Buyer"
  [b->s s->b quote-request order]
  (>!! b->s quote-request)
  (if (= (get-label (<!! s->b ["quote" "out-of-stock"])) "quote")
    (do (>!! b->s order)
        (<!! s->b "order-ack"))
    "Book is out of stock!"))

(defn- discourje-seller
  "Logic representing the Seller"
  [b->s s->b in-stock? quote order-ack out-of-stock]
  (if (== 1 (in-stock? (get-content (<!! b->s "quote-request"))))
    (do
      (>!! s->b quote)
      (<!! b->s "order")
      (>!! s->b order-ack))
    (>!! s->b out-of-stock)))

(defn discourje-one-buyer [iterations]
  (let [infra (vec (for [_ (range iterations)] (add-infrastructure buy-goods)))
        b->s (vec (for [i infra] (get-channel "buyer" "seller" i)))
        s->b (vec (for [i infra] (get-channel "seller" "buyer" i)))
        product {:product-type "book" :content {:title "The joy of Clojure"}}
        quote-request (msg "quote-request" product)
        order (msg "order" "confirm order!")
        in-stock? (fn [book] (rand-int 2))
        quote (msg "quote" "$40,00")
        order-ack (msg "order-ack" "order-ack confirmed!")
        out-of-stock (msg "out-of-stock" "Product out of stock!")
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
        b->s (get-channel "buyer" "seller" infra)
        s->b (get-channel "seller" "buyer" infra)
        product {:product-type "book" :content {:title "The joy of Clojure"}}
        quote-request (msg "quote-request" product)
        order (msg "order" "confirm order!")
        in-stock? (fn [book] (rand-int 2))
        quote (msg "quote" "$40,00")
        order-ack (msg "order-ack" "order-ack confirmed!")
        out-of-stock (msg "out-of-stock" "Product out of stock!")
        time (custom-time
               (doseq [_ (range iterations)]
                 (do
                   (clojure.core.async/thread (discourje-seller b->s s->b in-stock? quote order-ack out-of-stock))
                   (discourje-buyer b->s s->b quote-request order)
                   (force-monitor-reset! (get-monitor b->s)))))]

    time))

(defn- clojure-buyer
  "Logic representing Buyer"
  [b->s s->b quote-request order]
  (do (clojure.core.async/>!! b->s quote-request)
      (if (= (get-label (clojure.core.async/<!! s->b)) "quote")
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
        quote-request (msg "quote-request" product)
        order (msg "order" "confirm order!")
        in-stock? (fn [book] (rand-int 2))
        quote (msg "quote" "$40,00")
        order-ack (msg "order-ack" "order-ack confirmed!")
        out-of-stock (msg "out-of-stock" "Product out of stock!")
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
        b->s (get-channel "buyer" "seller" infra)
        s->b (get-channel "seller" "buyer" infra)
        product {:product-type "book" :content {:title "The joy of Clojure"}}
        quote-request (msg "quote-request" product)
        order (msg "order" "confirm order!")
        in-stock? (fn [book] (rand-int 2))
        quote (msg "quote" "$40,00")
        order-ack (msg "order-ack" "order-ack confirmed!")
        out-of-stock (msg "out-of-stock" "Product out of stock!")
        time (custom-time
               (doseq [_ (range iterations)]
                 (do
                   (clojure.core.async/thread (clojure-seller (get-chan b->s) (get-chan s->b) in-stock? quote order-ack out-of-stock))
                   (clojure-buyer (get-chan b->s) (get-chan s->b) quote-request order)
                   (force-monitor-reset! (get-monitor b->s)))))]
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