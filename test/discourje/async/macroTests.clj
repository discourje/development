(ns discourje.async.macroTests
  (:require [clojure.test :refer :all]
            [discourje.core.async :refer :all]))

(deftest atomic-interaction-test
  (is (= (assoc (make-interaction 1 "a" "b") :id 1) (assoc (-->> 1 "a" "b") :id 1))))

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

(deftest create-channel-test
  (let [fnChan (generate-channel "a" "b" 1)
        macroChan (create-channel "a" "b" 1)]
  (is (= (get-provider fnChan) (get-provider macroChan)))
  (is (= (get-consumer fnChan) (get-consumer macroChan)))
  (is (= (get-buffer fnChan) (get-buffer macroChan)))))

(def api-two-buyer-protocol
  (mep
    (rec :order-book
         (-->> "title" "Buyer1" "Seller")
         (-->> "quote" "Seller" ["Buyer1" "Buyer2"])
         (-->> "quoteDiv" "Buyer1" "Buyer2")
         (choice
           [(-->> "ok" "Buyer2" "Seller")
            (-->> "date" "Seller" "Buyer2")
            (continue :order-book)]
           [(-->> "quit" "Buyer2" "Seller")]))))

(deftest api-two-buyer-protocol-monitor-test
  (let [mon (generate-monitor api-two-buyer-protocol)]
    (is (= 1 (count (:interactions mon))))))

(deftest api-two-buyer-protocol-ids-test
  (let [mon (generate-monitor api-two-buyer-protocol)
        i0 (nth (:interactions mon) 0)
        i0r0i0 (nth (:recursion i0)0)
        i0r0i1 (nth (:recursion i0)1)
        i0r0i2 (nth (:recursion i0)2)
        i0r0i3 (nth (:recursion i0)3)
        i0r0i3b00 (nth (nth (:branches i0r0i3)0)0)
        i0r0i3b01 (nth (nth (:branches i0r0i3)0)1)
        i0r0i3b02 (nth (nth (:branches i0r0i3)0)2)
        i0r0i3b10 (nth (nth (:branches i0r0i3)1)0)]
    (is (= (get-next i0) nil))
    (is (= (get-next i0r0i0) (get-id i0r0i1)))
    (is (= (get-next i0r0i1) (get-id i0r0i2)))
    (is (= (get-next i0r0i2) (get-id i0r0i3)))
    (is (= (get-next i0r0i3) nil))
    (is (= (get-next i0r0i3b00) (get-id i0r0i3b01)))
    (is (= (get-next i0r0i3b01) (get-id i0r0i3b02)))
    (is (= (get-next i0r0i3b02) (get-id i0)))
    (is (= (get-next i0r0i3b10) nil))))

(deftest send-receive-two-buyer-protocol-test
  (let [channels (generate-infrastructure api-two-buyer-protocol)
        b1s (get-channel "Buyer1" "Seller" channels)
        sb1 (get-channel "Seller" "Buyer1" channels)
        sb2 (get-channel "Seller" "Buyer2" channels)
        b1b2 (get-channel "Buyer1" "Buyer2" channels)
        b2s (get-channel "Buyer2" "Seller" channels)
        order-book (atom true)]
    (while (true? @order-book)
      (do
        (>!! b1s (->message "title" "The Joy of Clojure"))
        (let [b1-title-s (<!! b1s "title")]
          (is (= "title" (get-label b1-title-s)))
          (is (= "The Joy of Clojure" (get-content b1-title-s)))
          (>!! [sb1 sb2] (->message "quote" (+ 1 (rand-int 20))))
          (let [s-quote-b1 (<!! sb1 "quote")
                s-quote-b2 (<!! sb2 "quote")]
            (is (= "quote" (get-label s-quote-b1)))
            (is (= "quote" (get-label s-quote-b2)))
            (>!! b1b2 (->message "quoteDiv" (rand-int (get-content s-quote-b1))))
            (let [b1-quoteDiv-b2 (<!! b1b2 "quoteDiv")]
              (is (= "quoteDiv" (get-label b1-quoteDiv-b2)))
              (if (>= (* 100 (float (/ (get-content b1-quoteDiv-b2) (get-content s-quote-b2)))) 50)
                (do
                  (>!! b2s (->message "ok" "Open University, Valkenburgerweg 177, 6419 AT, Heerlen"))
                  (let [b2-ok-s (<!! b2s "ok")]
                    (is (= "ok" (get-label b2-ok-s)))
                    (is (= "Open University, Valkenburgerweg 177, 6419 AT, Heerlen" (get-content b2-ok-s)))
                    (>!! sb2 (->message "date" "09-04-2019"))
                    (let [s-date-b2 (<!! sb2 "date")]
                      (is (= "date" (get-label s-date-b2)))
                      (is (= "09-04-2019" (get-content s-date-b2))))))
                (do
                  (>!! b2s (->message "quit" "Price to high"))
                  (let [b2-quit-s (<!! b2s "quit")]
                    (is (= "quit" (get-label b2-quit-s)))
                    (is (= "Price to high" (get-content b2-quit-s)))
                    (reset! order-book false)))))))))))
