(ns discourje.async.thesisExamplesDCA
  (:require [clojure.test :refer :all]
            [discourje.core.async :refer :all]))


(discourje.core.logging/set-logging-exceptions)

(def foo-bar-protocol
  (mep
    (-->> "Foo" "Alice" "Bob")
    (-->> "Bar" "Bob" "Alice")))

(def infra (add-infrastructure foo-bar-protocol))
(def alice-to-bob (get-channel infra "Alice" "Bob"))
(def bob-to-alice (get-channel infra "Bob" "Alice"))

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
(def buyer1-to-seller (get-channel  infra "Buyer1" "Seller"))
(def seller-to-buyer1 (get-channel  infra "Seller" "Buyer1"))

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
;------------------------------------------------------------------------------------------------------------------------
(def integer-protocol
  (mep
    (-->> java.lang.Integer "Alice" "Bob")
    (-->> java.lang.Integer "Bob" "Alice")
    (-->> java.lang.Integer "Alice" "Carol")
    (-->> java.lang.Integer "Carol" "Alice")))

(def infra (add-infrastructure integer-protocol))
(def alice-to-bob (get-channel infra "Alice" "Bob"))
(def alice-to-carol (get-channel infra "Alice" "Carol"))
(def carol-to-alice (get-channel infra"Carol" "Alice"))
(def bob-to-alice (get-channel infra "Bob" "Alice"))
;------------BEFORE-----------------
(defn alice []
  (>!! alice-to-bob (msg java.lang.Integer 1))
  (println "Alice received: " (get-content (<!! bob-to-alice java.lang.Integer)))
  (>!! alice-to-carol (msg java.lang.Integer 3))
  (println "Alice received: " (get-content (<!! carol-to-alice java.lang.Integer))))

(defn bob []
  (println "Bob received: " (get-content (<!! alice-to-bob java.lang.Integer)))
  (>!! bob-to-alice (msg java.lang.Integer 2)))

(defn carol []
  (println "Carol received: " (get-content (<!! alice-to-carol java.lang.Integer)))
  (>!! carol-to-alice (msg java.lang.Integer 4)))
;------------AFTER-----------------
(enable-wildcard)
(defn alice []
  (>!! alice-to-bob 1)
  (println "Alice received: " (get-content (<!! bob-to-alice)))
  (>!! alice-to-carol 3)
  (println "Alice received: " (get-content (<!! carol-to-alice))))

(defn bob []
  (println "Bob received: " (get-content (<!! alice-to-bob)))
  (>!! bob-to-alice 2))

(defn carol []
  (println "Carol received: " (get-content (<!! alice-to-carol)))
  (>!! carol-to-alice 4))

(defn multicast [interactions]
  interactions)

(def scatter-gather-multicast
  (mep
    (-->> 1 "master" ["worker0","worker1", "...", "workerN"])
    (multicast [(-->> 1 "worker0" "master")
                (-->> 1 "worker1" "master")
                (-->> 1 ".."      "master")
                (-->> 1 "workerN" "master")])))

(def scatter-gather-multicast-nested
  (mep
    (-->> 1 "master" ["worker0","worker1", "...", "workerN"])
    (multicast [[(-->> 1 "worker0" "master") (-->> "confirm" "master" "worker0")]
                [(-->> 1 "worker1" "master") (-->> "confirm" "master" "worker1")]
                [(-->> 1 ".."      "master") (-->> "confirm" "master" "...")]
                [(-->> 1 "workerN" "master") (-->> "confirm" "master" "workerN")]])))

(defmacro unless [condition body]
  `(if (not ~condition)
     (do ~@body)))

(defn without-macro []
  (if (not (< 6 5))
    (do (println "6 is not smaller than 5!"))))

(defn with-macro []
  (unless (< 6 5) (println "6 is not smaller than 5!")))

(without-macro)


(with-macro)