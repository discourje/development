(ns discourje.benchmarks.twoBuyer.dcaTwoBuyer
  (:require [discourje.core.async :refer :all]
            [discourje.core.logging :refer :all])
  (:use [slingshot.slingshot :only [throw+ try+]]))

(defn buyer1 "order a book from buyer1's perspective"
  [b1-s s-b1 b1-b2 title quote-div can-order?]
  (loop [it 0]
    (do
      (>!! b1-s title)
      (<!!! s-b1 "quote")
      (>!! b1-b2 quote-div)
      (<!!! s-b1 "date")
      (if (false? @can-order?)
        (println (+ 1 it))
        (recur (+ it 1))))))

(defn buyer2 "Order a book from buyer2's perspective"
  [s-b2 b1-b2 b2-s ok address can-order?]
  (loop []
    (do (<!!! s-b2 "quote")
        (<!!! b1-b2 "quote-div")
        (>!! b2-s ok)
        (>!! b2-s address)
        (<!!! s-b2 "date")
        (when (true? @can-order?) (recur)))))

(defn seller "Order book from seller's perspective"
  [b1-s s-b1 s-b2 b2-s quote date can-order?]
  (loop []
    (do (<!!! b1-s "title")
        (>!! [s-b1 s-b2] quote)
        (<!!! b2-s "ok")
        (<!!! b2-s "address")
        (>!! [s-b2 s-b1] date)
        (when (true? @can-order?) (recur)))))

(def two-buyer-protocol
  (mep
    (rec :order-book
         (-->> "title" "buyer1" "seller")
         (-->> "quote" "seller" ["buyer1" "buyer2"])
         (-->> "quote-div" "buyer1" "buyer2")
         (choice
           [(-->> "ok" "buyer2" "seller")
            (-->> "address" "buyer2" "seller")
            (-->> "date" "seller" ["buyer1" "buyer2"])
            (continue :order-book)]
           [(-->> "quit" "buyer2" "seller")]))))
(println (add-infrastructure two-buyer-protocol))

;generate the infra structure for the protocol
(set-logging-exceptions)
;start each participant on another thread
(defn start! [can-order?]
  (let [infra (add-infrastructure two-buyer-protocol)
        b1 (thread (buyer1 (get-channel "buyer1" "seller" infra)
                           (get-channel "seller" "buyer1" infra)
                           (get-channel "buyer1" "buyer2" infra)
                           (msg "title" "book")
                           (msg "quote-div" 16)
                           can-order?))
        b2 (thread (buyer2 (get-channel "seller" "buyer2" infra)
                           (get-channel "buyer1" "buyer2" infra)
                           (get-channel "buyer2" "seller" infra)
                           (msg "ok" "ok")
                           (msg "address" "address")
                           can-order?))
        s (thread (seller (get-channel "buyer1" "seller" infra)
                          (get-channel "seller" "buyer1" infra)
                          (get-channel "seller" "buyer2" infra)
                          (get-channel "buyer2" "seller" infra)
                          (msg "quote" 20)
                          (msg "date" 3)
                          can-order?))]
    (thread
      (do (Thread/sleep 5000)
          (reset! can-order? false)
          (clojure.core.async/close! b1)
          (clojure.core.async/close! b2)
          (clojure.core.async/close! s)))))

(dotimes [_ 10] (start! (atom true)))
