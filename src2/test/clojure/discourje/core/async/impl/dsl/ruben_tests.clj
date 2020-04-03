(ns discourje.core.async.impl.dsl.ruben-tests
  (:require [clojure.test :refer :all]
            [discourje.core.async.impl.dsl.abstract :refer :all]
            [discourje.core.async.impl.dsl.concrete :refer :all]))

(deftest- test-->
          (let [inter (-->> 1 "a" "b")]
            (is (= (true? ((get-predicate inter) Long))))))

(deftest- test-->withFunction
          (let [inter (-->> (fn [x] (+ x 1)) "a" "b")]
            (is (== 2 ((get-predicate inter) 1)))))

(deftest- test-->>withWildcard
          (let [inter (-->> "a" "b")]
            (is (= (true? ((get-predicate inter) "random value"))))))

(deftest close-test
  (is (= (assoc (make-closer "a" "b") :id 1) (assoc (close "a" "b") :id 1))))

(deftest mep-test
  (let [interaction (-->> 1 "A" "B")]
    (is (= (mep interaction) (create-protocol [interaction])))))

(deftest choice-test
  (let [i1 (-->> 1 "A" "B")
        i2 (-->> 2 "A" "B")
        i3 (-->> 3 "A" "B")]
    (is (= (assoc (make-choice [[i1] [i2] [i3]]) :id 1) (assoc (choice [i1] [i2] [i3]) :id 1)))))

(deftest continue-test
  (is (= (assoc (do-recur :test) :id 1) (assoc (continue :test) :id 1))))

(deftest recur-test
  (let [i1 (-->> 1 "A" "B")
        i2 (-->> 2 "A" "B")
        i3 (-->> 3 "A" "B")]
    (is (= (assoc (make-recursion :test [i1 i2 i3]) :id 1) (assoc (rec :test i1 i2 i3) :id 1)))))

(deftest parallel-test
  (let [i1 (-->> 1 "A" "B")
        i2 (-->> 2 "A" "B")
        i3 (-->> 3 "A" "B")]
    (is (= (assoc (make-parallel [[i1] [i2] [i3]]) :id 1) (assoc (parallel [i1] [i2] [i3]) :id 1)))))

;(def api-two-buyer-protocol
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
;(deftest send-receive-two-buyer-protocol-test
;  (let [infra (generate-infrastructure api-two-buyer-protocol)
;        b1s (get-channel infra "Buyer1" "Seller")
;        sb1 (get-channel infra "Seller" "Buyer1")
;        sb2 (get-channel infra "Seller" "Buyer2")
;        b1b2 (get-channel infra "Buyer1" "Buyer2")
;        b2s (get-channel infra "Buyer2" "Seller")
;        order-book (atom true)]
;    (while (true? @order-book)
;      (do
;        (>!! b1s (->message "title" "The Joy of Clojure"))
;        (let [b1-title-s (<!! b1s "title")]
;          (is (= "The Joy of Clojure"  b1-title-s))
;          (>!! [sb1 sb2] (->message "quote" (+ 1 (rand-int 20))))
;          (let [s-quote-b1 (<!! sb1 "quote")
;                s-quote-b2 (<!! sb2 "quote")]
;            (>!! b1b2 (->message "quoteDiv" (rand-int s-quote-b1)))
;            (let [b1-quoteDiv-b2 (<!! b1b2 "quoteDiv")]
;              (if (>= (* 100 (float (/ b1-quoteDiv-b2 s-quote-b2))) 50)
;                (do
;                  (>!! b2s (->message "ok" "Open University, Valkenburgerweg 177, 6419 AT, Heerlen"))
;                  (let [b2-ok-s (<!! b2s "ok")]
;                    (is (= "Open University, Valkenburgerweg 177, 6419 AT, Heerlen" b2-ok-s))
;                    (>!! sb2 (->message "date" "09-04-2019"))
;                    (let [s-date-b2 (<!! sb2 "date")]
;                      (is (= "09-04-2019" s-date-b2)))))
;                (do
;                  (>!! b2s (->message "quit" "Price to high"))
;                  (let [b2-quit-s (<!! b2s "quit")]
;                    (is (= "Price to high" b2-quit-s))
;                    (reset! order-book false)))))))))))