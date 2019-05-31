(ns discourje.benchmarks.TwoBuyer
  (:require [discourje.core.async :refer :all]
            [discourje.core.logging :refer :all])
  (:use [slingshot.slingshot :only [throw+ try+]]))

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

(defn discourje-buyer1 "order a book from buyer1's perspective"
  [b1-s s-b1 b1-b2 title div]
  (do
    (>!! b1-s title)
    (<!!! s-b1 "quote")
    (>!! b1-b2 div)))

(defn discourje-buyer2 "Order a book from buyer2's perspective"
  [s-b2 b1-b2 b2-s ok address]
  (do
    (<!!! s-b2 "quote")
    (<!!! b1-b2 "quote-div")
    (>!! b2-s ok)
    (>!! b2-s address)
    (<!!! s-b2 "date")))

(defn discourje-seller "Order book from seller's perspective"
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

(defn discourje-two-buyer []
  (let [infra (generate-infrastructure two-buyer-protocol)
        b1-s (get-channel "buyer1" "seller" infra)
        s-b1 (get-channel "seller" "buyer1" infra)
        b1-b2 (get-channel "buyer1" "buyer2" infra)
        s-b2 (get-channel "seller" "buyer2" infra)
        b2-s (get-channel "buyer2" "seller" infra)
        title (msg "title" "The Joy of Clojure")
        div (msg "quote-div" 16)
        ok (msg "ok" "ok")
        address (msg "address" "Open University, Valkenburgerweg 177, 6419 AT, Heerlen")
        quote (msg "quote" 15)
        date (msg "date" 1)]
    (time
      (do
        (thread (discourje-buyer1 b1-s s-b1 b1-b2 title div))
        (thread (discourje-seller b1-s s-b1 s-b2 b2-s quote date))
        (discourje-buyer2 s-b2 b1-b2 b2-s ok address)))
    (doseq [c infra] (clojure.core.async/close! (get-chan c)))))

(set-logging-exceptions)
(discourje-two-buyer)

(defn clojure-buyer1 "order a book from buyer1's perspective"
  [b1-s s-b1 b1-b2 title div]
  (do
    (clojure.core.async/>!! b1-s title)
    (clojure.core.async/<!! s-b1)
    (clojure.core.async/>!! b1-b2 div)))

(defn clojure-buyer2 "Order a book from buyer2's perspective"
  [s-b2 b1-b2 b2-s ok address]
  (do
    (clojure.core.async/<!! s-b2)
    (clojure.core.async/<!! b1-b2)
    (clojure.core.async/>!! b2-s ok)
    (clojure.core.async/>!! b2-s address)
    (clojure.core.async/<!! s-b2)))

(defn clojure-seller "Order book from seller's perspective"
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

(defn clojure-two-buyer []
  (let [b1-s (clojure.core.async/chan 1)
        s-b1 (clojure.core.async/chan 1)
        b1-b2 (clojure.core.async/chan 1)
        s-b2 (clojure.core.async/chan 1)
        b2-s (clojure.core.async/chan 1)
        title (msg "title" "The Joy of Clojure")
        div (msg "quote-div" 16)
        ok (msg "ok" "ok")
        address (msg "address" "Open University, Valkenburgerweg 177, 6419 AT, Heerlen")
        quote (msg "quote" 15)
        date (msg "date" 1)]
    (time
      (do
        (thread (clojure-buyer1 b1-s s-b1 b1-b2 title div))
        (thread (clojure-seller b1-s s-b1 s-b2 b2-s quote date))
        (clojure-buyer2 s-b2 b1-b2 b2-s ok address)))
    (clojure.core.async/close! b1-s)
    (clojure.core.async/close! s-b1)
    (clojure.core.async/close! b1-b2)
    (clojure.core.async/close! s-b2)
    (clojure.core.async/close! b2-s)))
(clojure-two-buyer)