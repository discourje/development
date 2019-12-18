(ns discourje.benchmarks.TwoBuyer
  (:require [discourje.core.async :refer :all]
            [discourje.core.logging :refer :all])
  (:import (clojure.lang PersistentArrayMap)
           (java.util GregorianCalendar)))

(def two-buyer-protocol
  (mep
    (-->> String "buyer1" "seller")
    (-->> Long "seller" ["buyer1" "buyer2"])
    (-->> Long "buyer1" "buyer2")
    (choice
      [(-->> PersistentArrayMap "buyer2" "seller")
       (-->> Long "buyer2" "seller")
       (-->> Long "seller" "buyer2")]
      [(-->> PersistentArrayMap "buyer2" "seller")])))

(defn- discourje-buyer1 "order a book from buyer1's perspective"
  [b1-s s-b1 b1-b2 title div]
  (do
    (>!! b1-s title)
    (<!!8 s-b1)
    (>!! b1-b2 div)))

(defn- discourje-buyer2 "Order a book from buyer2's perspective"
  [s-b2 b1-b2 b2-s ok address]
  (do
    (<!!8 s-b2)
    (<!!8 b1-b2)
    (>!! b2-s ok)
    (>!! b2-s address)
    (<!!8 s-b2)))

(defn- discourje-seller "Order book from seller's perspective"
  [b1-s s-b1 s-b2 b2-s quote date]
  (do
    (<!! b1-s)
    (>!! [s-b1 s-b2] quote)
    (let [choice-by-buyer2 (<!! b2-s)]
      (cond
        (= "ok" (:choice choice-by-buyer2))
        (do
          (<!! b2-s)
          (>!! s-b2 date))
        (= "quit" (:choice choice-by-buyer2))
        "Quit!"))))

(defn discourje-two-buyer [iterations]
  (time(let [infra (vec (for [_ (range iterations)] (generate-infrastructure two-buyer-protocol)))
             b1-s (vec (for [i infra] (get-channel i "buyer1" "seller")))
             s-b1 (vec (for [i infra] (get-channel i "seller" "buyer1")))
             b1-b2 (vec (for [i infra] (get-channel i "buyer1" "buyer2")))
             s-b2 (vec (for [i infra] (get-channel i "seller" "buyer2")))
             b2-s (vec (for [i infra] (get-channel i "buyer2" "seller")))
             title "The Joy of Clojure"
             div  16
             ok {:choice "ok" :content "ok"}
             address "Open University, Valkenburgerweg 177, 6419 AT, Heerlen"
             quote  15
             date 1
             time (time
                    (doseq [i (range iterations)]
                      (do
                        (thread (discourje-buyer1 (nth b1-s i) (nth s-b1 i) (nth b1-b2 i) title div))
                        (thread (discourje-seller (nth b1-s i) (nth s-b1 i) (nth s-b2 i) (nth b2-s i) quote date))
                        (discourje-buyer2 (nth s-b2 i) (nth b1-b2 i) (nth b2-s i) ok address))))]
         (doseq [i infra] (doseq [c i] (clojure.core.async/close! (get-chan c))))
         time)))

(defn discourje-two-buyer-monitor-reset [iterations]
  (let [infra (generate-infrastructure two-buyer-protocol)
        b1-s (get-channel infra "buyer1" "seller")
        s-b1 (get-channel infra "seller" "buyer1")
        b1-b2 (get-channel infra "buyer1" "buyer2")
        s-b2 (get-channel infra "seller" "buyer2")
        b2-s (get-channel infra "buyer2" "seller")
        title  "The Joy of Clojure"
        interactions (get-active-interaction (get-monitor b1-s))
        div  16
        ok  {:choice "ok" :content "ok"}
        address  "Open University, Valkenburgerweg 177, 6419 AT, Heerlen"
        quote 15
        date  1
        time (custom-time
               (doseq [_ (range iterations)]
                 (do
                   (thread (discourje-buyer1 b1-s s-b1 b1-b2 title div))
                   (thread (discourje-seller b1-s s-b1 s-b2 b2-s quote date))
                   (discourje-buyer2 s-b2 b1-b2 b2-s ok address)
                   (force-monitor-reset! (get-monitor b1-s) interactions))))]
    time))

(defn- clojure-buyer2 "Order a book from buyer2's perspective"
  [s-b2 b1-b2 b2-s ok address]
  (do
    (clojure.core.async/<!! s-b2)
    (clojure.core.async/<!! b1-b2)
    (clojure.core.async/>!! b2-s ok)
    (clojure.core.async/>!! b2-s address)
    (clojure.core.async/<!! s-b2)))

(defn- clojure-buyer1 "order a book from buyer1's perspective"
  [b1-s s-b1 b1-b2 title div]
  (do
    (clojure.core.async/>!! b1-s title)
    (clojure.core.async/<!! s-b1)
    (clojure.core.async/>!! b1-b2 div)))

(defn- clojure-seller "Order book from seller's perspective"
  [b1-s s-b1 s-b2 b2-s quote date]
  (do
    (clojure.core.async/<!! b1-s)
    (clojure.core.async/>!! s-b1 quote)
    (clojure.core.async/>!! s-b2 quote)
    (let [choice-by-buyer2 (clojure.core.async/<!! b2-s)]
      (cond
        (= "ok" choice-by-buyer2)
        (do
          (clojure.core.async/<!! b2-s)
          (clojure.core.async/>!! s-b2 date))
        (= "quit" choice-by-buyer2)
        "Quit!"))))

(defn clojure-two-buyer [iterations]
  (let [b1-s (vec (for [_ (range iterations)] (clojure.core.async/chan 1)))
        s-b1 (vec (for [_ (range iterations)] (clojure.core.async/chan 1)))
        b1-b2 (vec (for [_ (range iterations)] (clojure.core.async/chan 1)))
        s-b2 (vec (for [_ (range iterations)] (clojure.core.async/chan 1)))
        b2-s (vec (for [_ (range iterations)] (clojure.core.async/chan 1)))
        title "The Joy of Clojure"
        div 16
        ok "ok"
        address "Open University, Valkenburgerweg 177, 6419 AT, Heerlen"
        quote  15
        date  1
        time (custom-time
               (doseq [i (range iterations)]
                 (do
                   (clojure.core.async/thread (clojure-buyer1 (nth b1-s i) (nth s-b1 i) (nth b1-b2 i) title div))
                   (clojure.core.async/thread (clojure-seller (nth b1-s i) (nth s-b1 i) (nth s-b2 i) (nth b2-s i) quote date))
                   (clojure-buyer2 (nth s-b2 i) (nth b1-b2 i) (nth b2-s i) ok address))))]
    (doseq [i (range iterations)]
      (clojure.core.async/close! (nth b1-s i))
      (clojure.core.async/close! (nth s-b1 i))
      (clojure.core.async/close! (nth b1-b2 i))
      (clojure.core.async/close! (nth s-b2 i))
      (clojure.core.async/close! (nth b2-s i)))
    time))

(defn clojure-two-buyer-reset [iterations]
  (let [infra (generate-infrastructure two-buyer-protocol)
        b1-s (get-channel infra "buyer1" "seller")
        s-b1 (get-channel infra "seller" "buyer1")
        b1-b2 (get-channel infra "buyer1" "buyer2")
        s-b2 (get-channel infra "seller" "buyer2")
        b2-s (get-channel infra "buyer2" "seller")
        title "The Joy of Clojure"
        interactions (get-active-interaction (get-monitor b1-s))
        div  16
        ok "ok"
        address "Open University, Valkenburgerweg 177, 6419 AT, Heerlen"
        quote  15
        date  1
        time (custom-time
               (doseq [_ (range iterations)]
                 (do
                   (clojure.core.async/thread (clojure-buyer1 (get-chan b1-s) (get-chan s-b1) (get-chan b1-b2) title div))
                   (clojure.core.async/thread (clojure-seller (get-chan b1-s) (get-chan s-b1) (get-chan s-b2) (get-chan b2-s) quote date))
                   (clojure-buyer2 (get-chan s-b2) (get-chan b1-b2) (get-chan b2-s) ok address)
                   (force-monitor-reset! (get-monitor b1-s) interactions))))]
    time))
(clojure-two-buyer-reset 100)