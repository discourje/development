(ns discourje.async.thesisExamplesDCA
  (:require [clojure.test :refer :all]
            [discourje.core.async :refer :all]))

(def protocol
  (mep
    (-->> "Title" "Buyer1" "Seller")
    (-->> "Quote" "Seller" "Buyer1")))

(def infra (add-infrastructure protocol))
(def buyer1-to-seller (get-channel "Buyer1" "Seller" infra))
(def seller-to-buyer1 (get-channel "Seller" "Buyer1" infra))

(defn buyer1 []
  (println "Buyer1 to request quote for book.")
  (>!! buyer1-to-seller (msg "Title" "The Joy of Clojure"))
  (println "The quote for the book is: " (get-content (<!! seller-to-buyer1 "Quote"))))

(defn seller []
  (let [book (get-content (<!! buyer1-to-seller "Title"))]
    (println (format "Received book: %s, returning price." book))
    (>!! seller-to-buyer1 (msg "Quote" "$40.00"))))

(thread (buyer1))
(thread (seller))
(discourje.core.logging/set-logging-exceptions)