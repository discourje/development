(ns discourje.async.macroTests
  (:require [clojure.test :refer :all]
            [discourje.core.async.async :refer :all]))

(deftest mep-test
  (let [interaction (-->> 1 "A" "B")]
  (is (= (mep interaction) (create-protocol [interaction])))))

(deftest choice-test
  (let [i1 (-->> 1 "A" "B")
        i2 (-->> 2 "A" "B")
        i3 (-->> 3 "A" "B")]
    (is (= (assoc (make-choice [[i1][i2][i3]]) :id 1) (assoc (choice [i1][i2][i3]) :id 1)))))

(deftest continue-test
  (is (= (assoc (do-recur :test) :id 1) (assoc (continue :test) :id 1))))

(deftest recur-test
  (let [i1 (-->> 1 "A" "B")
        i2 (-->> 2 "A" "B")
        i3 (-->> 3 "A" "B")]
    (is (= (assoc (make-recursion :test [i1 i2 i3]) :id 1) (assoc (rec :test i1 i2 i3) :id 1)))))


;
;(defn api-two-buyer-protocol []
;  (mep
;    (rec :order-book
;         (-->> "title" "Buyer1" "Seller")
;         (-->> "quote" "Seller" ["Buyer1" "Buyer2"])
;         (-->> "quoteDiv" "Buyer1" "Buyer2")
;         (choice
;           [(-->> "ok" "Buyer2" "Seller")
;            (-->> "date" "Seller" "Buyer2")
;            (continue :order-book)]
;           [(-->> "quit" "Buyer2" "Seller")]))))
;
;(println (api-two-buyer-protocol))