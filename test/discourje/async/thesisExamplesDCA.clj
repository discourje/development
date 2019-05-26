(ns discourje.async.thesisExamplesDCA
  (:require [clojure.test :refer :all]
            [discourje.core.async :refer :all]))


(discourje.core.logging/set-logging-exceptions)

(def foo-bar-protocol
  (mep
    (-->> "Foo" "Alice" "Bob")
    (-->> "Bar" "Bob" "Alice")))

(def infra (add-infrastructure foo-bar-protocol))
(def alice-to-bob (get-channel "Alice" "Bob" infra))
(def bob-to-alice (get-channel "Bob" "Alice" infra))

(defn alice []
  (>!! alice-to-bob (msg "Foo" "Foo content"))
  (println "Alice received: " (get-content (<!! bob-to-alice "Bar"))))

(defn bob []
  (println "Bob received: " (get-content (<!! alice-to-bob "Foo")))
  (>!! bob-to-alice (msg "Bar" "Bar content")))

(thread (alice))
(thread (bob))


;----------------------------------------------------------------------------------------------------------------------
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