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


;(deftest api-two-buyer-protocol-monitor-test
;  (let [mon (generate-monitor (api-two-buyer-protocol))]
;    (is (= 1 (count (:interactions mon))))))
;
;(deftest api-two-buyer-protocol-ids-test
;  (let [mon (generate-monitor (api-two-buyer-protocol))
;        i0 (nth (:interactions mon) 0)
;        i0r0i0 (nth (:recursion i0)0)
;        i0r0i1 (nth (:recursion i0)1)
;        i0r0i2 (nth (:recursion i0)2)
;        i0r0i3 (nth (:recursion i0)3)
;        i0r0i3b00 (nth (nth (:branches i0r0i3)0)0)
;        i0r0i3b01 (nth (nth (:branches i0r0i3)0)1)
;        i0r0i3b02 (nth (nth (:branches i0r0i3)0)2)
;        i0r0i3b10 (nth (nth (:branches i0r0i3)1)0)
;        ]
;    (println (:interactions mon))
;    (is (= (get-next i0) nil))
;    (is (= (get-next i0r0i0) (get-id i0r0i1)))
;    (is (= (get-next i0r0i1) (get-id i0r0i2)))
;    (is (= (get-next i0r0i2) (get-id i0r0i3)))
;    (is (= (get-next i0r0i3) nil))
;    (is (= (get-next i0r0i3b00) (get-id i0r0i3b01)))
;    (is (= (get-next i0r0i3b01) (get-id i0r0i3b02)))
;    (is (= (get-next i0r0i3b02) (get-id i0)))
;    (is (= (get-next i0r0i3b10) nil))
;    ))

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