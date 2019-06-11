(ns discourje.benchmarks.TwoBuyer
  (:require [discourje.core.async :refer :all]
            [discourje.core.logging :refer :all]))

(def two-buyer-protocol
  (mep
    (-->> "title" "buyer1" "seller")
    (-->> "quote" "seller" ["buyer1" "buyer2"])
    (-->> "quote-div" "buyer1" "buyer2")
    (choice
      [(-->> "ok" "buyer2" "seller")
       (-->> "address" "buyer2" "seller")
       (-->> "date" "seller" "buyer2")]
      [(-->> "quit" "buyer2" "seller")])))

(defn- discourje-buyer1 "order a book from buyer1's perspective"
  [b1-s s-b1 b1-b2 title div]
  (do
    (>!! b1-s title)
    (<!!! s-b1 "quote")
    (>!! b1-b2 div)))

(defn- discourje-buyer2 "Order a book from buyer2's perspective"
  [s-b2 b1-b2 b2-s ok address]
  (do
    (<!!! s-b2 "quote")
    (<!!! b1-b2 "quote-div")
    (>!! b2-s ok)
    (>!! b2-s address)
    (<!!! s-b2 "date")))

(defn- discourje-seller "Order book from seller's perspective"
  [b1-s s-b1 s-b2 b2-s quote date]
  (do
    (<!! b1-s "title")
    (>!! [s-b1 s-b2] quote)
    (let [choice-by-buyer2 (<!! b2-s ["ok" "quit"])]
      (cond
        (= "ok" (get-label choice-by-buyer2))
        (do
          (<!! b2-s "address")
          (>!! s-b2 date))
        (= "quit" (get-label choice-by-buyer2))
        "Quit!"))))

(defn discourje-two-buyer [iterations]
  (let [infra (vec (for [_ (range iterations)] (generate-infrastructure two-buyer-protocol)))
        b1-s (vec (for [i infra] (get-channel "buyer1" "seller" i)))
        s-b1 (vec (for [i infra] (get-channel "seller" "buyer1" i)))
        b1-b2 (vec (for [i infra] (get-channel "buyer1" "buyer2" i)))
        s-b2 (vec (for [i infra] (get-channel "seller" "buyer2" i)))
        b2-s (vec (for [i infra] (get-channel "buyer2" "seller" i)))
        title (msg "title" "The Joy of Clojure")
        div (msg "quote-div" 16)
        ok (msg "ok" "ok")
        address (msg "address" "Open University, Valkenburgerweg 177, 6419 AT, Heerlen")
        quote (msg "quote" 15)
        date (msg "date" 1)
        time (custom-time
               (doseq [i (range iterations)]
                 (do
                   (thread (discourje-buyer1 (nth b1-s i) (nth s-b1 i) (nth b1-b2 i) title div))
                   (thread (discourje-seller (nth b1-s i) (nth s-b1 i) (nth s-b2 i) (nth b2-s i) quote date))
                   (discourje-buyer2 (nth s-b2 i) (nth b1-b2 i) (nth b2-s i) ok address))))]
    (doseq [i infra] (doseq [c i] (clojure.core.async/close! (get-chan c))))
    time))

;(set-logging-exceptions)
;(discourje-two-buyer 1)
;(discourje-two-buyer 2)
;(discourje-two-buyer 4)
;(discourje-two-buyer 8)
;(discourje-two-buyer 16)
;(discourje-two-buyer 32)
;(discourje-two-buyer 64)
;(discourje-two-buyer 128)
;(discourje-two-buyer 256)

(defn- clojure-buyer1 "order a book from buyer1's perspective"
  [b1-s s-b1 b1-b2 title div]
  (do
    (clojure.core.async/>!! b1-s title)
    (clojure.core.async/<!! s-b1)
    (clojure.core.async/>!! b1-b2 div)))

(defn- clojure-buyer2 "Order a book from buyer2's perspective"
  [s-b2 b1-b2 b2-s ok address]
  (do
    (clojure.core.async/<!! s-b2)
    (clojure.core.async/<!! b1-b2)
    (clojure.core.async/>!! b2-s ok)
    (clojure.core.async/>!! b2-s address)
    (clojure.core.async/<!! s-b2)))

(defn- clojure-seller "Order book from seller's perspective"
  [b1-s s-b1 s-b2 b2-s quote date]
  (do
    (clojure.core.async/<!! b1-s)
    (clojure.core.async/>!! s-b1 quote)
    (clojure.core.async/>!! s-b2 quote)
    (let [choice-by-buyer2 (clojure.core.async/<!! b2-s)]
      (cond
        (= "ok" (get-label choice-by-buyer2))
        (do
          (clojure.core.async/<!! b2-s)
          (clojure.core.async/>!! s-b2 date))
        (= "quit" (get-label choice-by-buyer2))
        "Quit!"))))

(defn clojure-two-buyer [iterations]
  (let [b1-s (vec (for [_ (range iterations)] (clojure.core.async/chan 1)))
        s-b1 (vec (for [_ (range iterations)] (clojure.core.async/chan 1)))
        b1-b2 (vec (for [_ (range iterations)] (clojure.core.async/chan 1)))
        s-b2 (vec (for [_ (range iterations)] (clojure.core.async/chan 1)))
        b2-s (vec (for [_ (range iterations)] (clojure.core.async/chan 1)))
        title (msg "title" "The Joy of Clojure")
        div (msg "quote-div" 16)
        ok (msg "ok" "ok")
        address (msg "address" "Open University, Valkenburgerweg 177, 6419 AT, Heerlen")
        quote (msg "quote" 15)
        date (msg "date" 1)
        time (custom-time
               (doseq [i (range iterations)]
                 (do
                   (thread (clojure-buyer1 (nth b1-s i) (nth s-b1 i) (nth b1-b2 i) title div))
                   (thread (clojure-seller (nth b1-s i) (nth s-b1 i) (nth s-b2 i) (nth b2-s i) quote date))
                   (clojure-buyer2 (nth s-b2 i) (nth b1-b2 i) (nth b2-s i) ok address))))]
    (doseq [i (range iterations)]
      (clojure.core.async/close! (nth b1-s i))
      (clojure.core.async/close! (nth s-b1 i))
      (clojure.core.async/close! (nth b1-b2 i))
      (clojure.core.async/close! (nth s-b2 i))
      (clojure.core.async/close! (nth b2-s i)))
    time))
;(clojure-two-buyer 1)
;(clojure-two-buyer 2)
;(clojure-two-buyer 4)
;(clojure-two-buyer 8)
;(clojure-two-buyer 16)
;(clojure-two-buyer 32)
;(clojure-two-buyer 64)
;(clojure-two-buyer 128)
;(clojure-two-buyer 256)