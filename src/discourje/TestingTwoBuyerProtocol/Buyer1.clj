(ns discourje.TestingTwoBuyerProtocol.Buyer1
  (:require [discourje.core.async :refer :all]
            [discourje.core.logging :refer :all])
  (:use [slingshot.slingshot :only [throw+ try+]]))

(def books ["The Joy of Clojure" "Mastering Clojure Macros" "Programming Clojure"])

(defn generate-book "generate book title" []
  (first (shuffle books)))

(defn quote-div "Generate random number with max, quote value" [quote]
  (log-message (format "received quote: %s" quote))
  (let [randomN (+ (rand-int quote) 1)]
    (log-message "QD = " randomN)
    randomN))

(defn order-book "order a book from buyer1's perspective" [infra]
  (let [b1-s (get-channel "buyer1" "seller" infra)
        s-b1 (get-channel "seller" "buyer1" infra)
        b1-b2 (get-channel "buyer1" "buyer2" infra)
        b2-b1 (get-channel "buyer2" "buyer1" infra)
        title-delivered? (atom false)
        quote-div-delivered? (atom false)]
    (while (false? @title-delivered?)
      (try+ (>!! b1-s (msg "title" (generate-book)))
            (reset! title-delivered? true)
            (catch [:type :incorrect-communication] {}
              (println "title not delivered, retrying in 1 second!")
              (Thread/sleep 1000))))
    (let [quote (<!!! s-b1 "quote")
          div (quote-div (get-content quote))]
      (do
        ;try catch here!
        (while (false? @quote-div-delivered?)
          (try+
            (println "sending quotediv")
            (>!! b1-b2 (msg "quote-div" div))
            (reset! quote-div-delivered? true)
            (catch [:type :incorrect-communication] {}
              (println "quote-div not delivered, retrying in 1 second!")
              (Thread/sleep 1000))))
        (when (<!!! b2-b1 "repeat")
          (order-book infra))))))

;(defn order-book "order a book from buyer1's perspective" [infra]
;  (let [b1-s (get-channel "buyer1" "seller" infra)
;        s-b1 (get-channel "seller" "buyer1" infra)
;        b1-b2 (get-channel "buyer1" "buyer2" infra)
;        b2-b1 (get-channel "buyer2" "buyer1" infra)]
;    (>!!! b1-s (msg "title" (generate-book)))
;    (let [quote (<!! s-b1 "quote")]
;      (do (>!!! b1-b2 (msg "quote-div" (quote-div (get-content quote))))
;      (when (<!!! b2-b1 "repeat")
;        (order-book infra))))))

;send title to seller
;wait for quote
;send quote div to buyer2