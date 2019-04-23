(ns discourje.async.thesisExamplesCCA
  (:require [clojure.test :refer :all]
            [clojure.core.async :refer :all]))

(def buyer1-to-seller (chan 1))
(def seller-to-buyer1 (chan 1))

(defn buyer1 []
  (println "Buyer1 to request quote for book.")
  (>!! buyer1-to-seller "The Joy of Clojure")
  (println "The quote for the book is: " (<!! seller-to-buyer1)))

(defn seller []
  (let [book (<!! buyer1-to-seller)]
    (println (format "Received book: %s, returning price." book))
    (>!! seller-to-buyer1 "$40.00")))

(thread (buyer1))
(thread (seller))
