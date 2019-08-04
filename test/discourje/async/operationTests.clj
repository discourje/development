(ns discourje.async.operationTests
  (:require [clojure.test :refer :all]
            [discourje.async.protocolTestData :refer :all]
            [discourje.core.async :refer :all]
            [clojure.core.async :as async]
            [discourje.core.logging :refer :all]))

(deftest send-test
  (let [channels (generate-infrastructure (testDualProtocol true))
        c (get-channel "A" "B" channels)]
    (>!! c (->message "1" "hello world"))
    (let [m (async/<!! (get-chan c))]
      (is (= "1" (get-label m)))
      (is (= "hello world" (get-content m))))))

(deftest receive-test
  (let [channels (generate-infrastructure (testDualProtocol true))
        c (get-channel "A" "B" channels)]
    (async/>!! (get-chan c) (->message "1" "hello world"))
    (let [m (<!! c "1")]
      (is (= "1" (get-label m)))
      (is (= "hello world" (get-content m))))))

(deftest send-receive-dual-test
  (let [channels (generate-infrastructure (testDualProtocol true))
        ab (get-channel "A" "B" channels)
        ba (get-channel "B" "A" channels)
        m1 (->message "1" "Hello B")
        m2 (->message "2" "Hello A")]
    (set-logging-exceptions)
    (do
      (>!! ab m1)
      (let [a->b (<!! ab "1")]
        (is (= "1" (get-label a->b)))
        (is (= "Hello B" (get-content a->b))))
      (>!! ba m2)
      (let [b->a (<!! ba "2")]
        (is (= "2" (get-label b->a)))
        (is (= "Hello A" (get-content b->a)))))))

(deftest send-receive-typed-dual-test
  (let [channels (generate-infrastructure (test-typed-DualProtocol true))
        ab (get-channel "A" "B" channels)
        ba (get-channel "B" "A" channels)]
    (do
      (>!! ab "Hello B")
      (let [a->b (<!! ab java.lang.String)]
        (is (= java.lang.String (get-label a->b)))
        (is (= "Hello B" (get-content a->b))))
      (>!! ba "Hello A")
      (let [b->a (<!! ba java.lang.String)]
        (is (= java.lang.String (get-label b->a)))
        (is (= "Hello A" (get-content b->a)))))))

(deftest send-receive-wildcard-dual-test
  (let [channels (generate-infrastructure (test-typed-DualProtocol true))
        ab (get-channel "A" "B" channels)
        ba (get-channel "B" "A" channels)]
    (do
      (enable-wildcard)
      (>!! ab "Hello B")
      (let [a->b (<!! ab)]
        (is (= java.lang.String (get-label a->b)))
        (is (= "Hello B" (get-content a->b))))
      (>!! ba "Hello A")
      (let [b->a (<!! ba)]
        (is (= java.lang.String (get-label b->a)))
        (is (= "Hello A" (get-content b->a)))
        ))))

(deftest send-receive-parallel-protocol-test
  (let [channels (generate-infrastructure (testParallelProtocol true))
        ab (get-channel "A" "B" channels)
        ba (get-channel "B" "A" channels)
        ac (get-channel "A" "C" channels)
        ca (get-channel "C" "A" channels)
        cb (get-channel "C" "B" channels)]
    (do
      (>!! ab (->message "1" "A->B"))
      (let [a->b (<!! ab "1")]
        (is (= "1" (get-label a->b)))
        (is (= "A->B" (get-content a->b)))
        (>!! ba (->message "2" "B->A"))
        (let [b->a (<!! ba "2")]
          (is (= "2" (get-label b->a)))
          (is (= "B->A" (get-content b->a)))
          (>!! ac (->message "3" "A->C"))
          (let [a->c (<!! ac "3")]
            (is (= "3" (get-label a->c)))
            (is (= "A->C" (get-content a->c)))
            (>!! [ca cb] (->message "4" "C->A-B"))
            (let [c->a (<!! ca "4")
                  c->b (<!! cb "4")]
              (is (= "4" (get-label c->a)))
              (is (= "C->A-B" (get-content c->a)))
              (is (= "4" (get-label c->b)))
              (is (= "C->A-B" (get-content c->b))))))))))

(deftest send-receive-parallel-wildcard-only-protocol-test
  (let [channels (generate-infrastructure (testParallelProtocol true))
        ab (get-channel "A" "B" channels)
        ba (get-channel "B" "A" channels)
        ac (get-channel "A" "C" channels)
        ca (get-channel "C" "A" channels)
        cb (get-channel "C" "B" channels)]
    (do
      (enable-wildcard)
      (>!! ab (->message "1" "A->B"))
      (let [a->b (<!! ab)]
        (is (= "1" (get-label a->b)))
        (is (= "A->B" (get-content a->b)))
        (>!! ba (->message "2" "B->A"))
        (let [b->a (<!! ba)]
          (is (= "2" (get-label b->a)))
          (is (= "B->A" (get-content b->a)))
          (>!! ac (->message "3" "A->C"))
          (let [a->c (<!! ac)]
            (is (= "3" (get-label a->c)))
            (is (= "A->C" (get-content a->c)))
            (>!! [ca cb] (->message "4" "C->A-B"))
            (let [c->a (<!! ca)
                  c->b (<!! cb)]
              (is (= "4" (get-label c->a)))
              (is (= "C->A-B" (get-content c->a)))
              (is (= "4" (get-label c->b)))
              (is (= "C->A-B" (get-content c->b))))))))))

(deftest send-receive-single-parallel-test
  (let [channels (generate-infrastructure (testSingleParallelProtocol))
        ab (get-channel "A" "B" channels)
        ac (get-channel "A" "C" channels)
        m1 (->message "1" "Hello B and C")]
    (do
      (>!! [ab ac] m1)
      (let [a->b (<!! ab "1")
            a->c (<!! ac "1")]
        (is (= "1" (get-label a->b)))
        (is (= "Hello B and C" (get-content a->b)))
        (is (= (get-label a->c) (get-label a->b)))
        (is (= (get-content a->c) (get-content a->b)))))))

(deftest send-receive-single-Always0-choice-protocol
  (let [channels (generate-infrastructure (single-choice-protocol))
        ab (get-channel "A" "B" channels)
        ma (->message "1" "Hello B")]
    (do
      (>!! ab ma)
      (let [a->b (<!! ab "1")]
        (is (= "1" (get-label a->b)))
        (is (= "Hello B" (get-content a->b)))))))

(deftest send-receive-single-always1-choice-protocol
  (let [channels (generate-infrastructure (single-choice-protocol))
        ac (get-channel "A" "C" channels)
        mc (->message "hi" "Hi C")]
    (do
      (>!! ac mc)
      (let [a->c (<!! ac "hi")]
        (is (= "hi" (get-label a->c)))
        (is (= "Hi C" (get-content a->c)))))))

(deftest send-receive-single-Random-choice-protocol
  (let [channels (generate-infrastructure (single-choice-protocol))
        ab (get-channel "A" "B" channels)
        ac (get-channel "A" "C" channels)
        ma (->message "1" "Hello B")
        mc (->message "hi" "Hi C")]
    (if (== 0 (rand-int 2))
      (do
        (>!! ab ma)
        (let [a->b (<!! ab "1")]
          (is (= "1" (get-label a->b)))
          (is (= "Hello B" (get-content a->b)))))
      (do
        (>!! ac mc)
        (let [a->c (<!! ac "hi")]
          (is (= "hi" (get-label a->c)))
          (is (= "Hi C" (get-content a->c))))))))

(deftest send-receive-multiple-nested-choice-branch-protocol
  (let [channels (generate-infrastructure (multiple-nested-choice-branch-protocol true))
        ab (get-channel "A" "B" channels)
        n (+ 1 (rand-int 4))]
    (cond
      (== n 1)
      (do (>!! ab (->message "1" "AB"))
          (let [a->b (<!! ab "1")]
            (is (= "1" (get-label a->b)))
            (is (= "AB" (get-content a->b)))))
      (== n 2)
      (do (>!! ab (->message "2" "AB"))
          (let [a->b (<!! ab "2")]
            (is (= "2" (get-label a->b)))
            (is (= "AB" (get-content a->b)))))
      (== n 3)
      (do (>!! ab (->message "3" "AB"))
          (let [a->b (<!! ab "3")]
            (is (= "3" (get-label a->b)))
            (is (= "AB" (get-content a->b)))))
      (== n 4)
      (do (>!! ab (->message "4" "AB"))
          (let [a->b (<!! ab "4")]
            (is (= "4" (get-label a->b)))
            (is (= "AB" (get-content a->b))))))))


(deftest send-receive-single-choice-in-middle-always0-choice-protocol
  (let [channels (generate-infrastructure (single-choice-in-middle-protocol true))
        sf (get-channel "Start" "Finish" channels)
        fs (get-channel "Finish" "Start" channels)
        ab (get-channel "A" "B" channels)
        ba (get-channel "B" "A" channels)
        msf (->message "99" "Starting!")
        mab (->message "1" "1B")
        mba (->message "bla" "blaA")
        mfs (->message "88" "ending!")]
    (set-logging-and-exceptions)
    (do
      (>!! sf msf)
      (let [s->f (<!! sf "99")]
        (is (= "99" (get-label s->f)))
        (is (= "Starting!" (get-content s->f)))
        (>!! ab mab)
        (let [a->b (<!! ab "1")]
          (is (= "1" (get-label a->b)))
          (is (= "1B" (get-content a->b)))
          (>!! ba mba)
          (let [b->a (<!! ba "bla")]
            (is (= "bla" (get-label b->a)))
            (is (= "blaA" (get-content b->a)))
            (>!! fs mfs)
            (let [f->s (<!! fs "88")]
              (is (= "88" (get-label f->s)))
              (is (= "ending!" (get-content f->s)))))
          )))))

(deftest send-receive-single-choice-multiple-interactions-protocol-test
  (let [channels (generate-infrastructure (single-choice-multiple-interactions-protocol true))
        ab (get-channel "A" "B" channels)
        ba (get-channel "B" "A" channels)
        ac (get-channel "A" "C" channels)
        ca (get-channel "C" "A" channels)
        ad (get-channel "A" "D" channels)
        da (get-channel "D" "A" channels)
        mab1 (->message "1" "1ab")
        mab-c2 (->message "2" "B or C")
        mab-c3 (->message "3" "B or C")
        mad (->message "4" "4d")
        m5 (->message "5" "bye all")]
    (do (>!! ab mab1)
        (let [a->b (<!! ab "1")]
          (is (= "1" (get-label a->b)))
          (is (= "1ab" (get-content a->b)))
          (>!! ba mab1)
          (let [b->a (<!! ba "1")]
            (is (= "1" (get-label b->a)))
            (is (= "1ab" (get-content b->a)))
            (>!! ac mab-c2)
            (let [a->c (<!! ac "2")]
              (is (= "2" (get-label a->c)))
              (is (= "B or C" (get-content a->c)))
              (>!! ca mab-c2)
              (let [c->a (<!! ca "2")]
                (is (= "2" (get-label c->a)))
                (is (= "B or C" (get-content c->a)))
                (>!! ac mab-c3)
                (let [a->c3 (<!! ac "3")]
                  (is (= "3" (get-label a->c3)))
                  (is (= "B or C" (get-content a->c3)))
                  (>!! ca mab-c3)
                  (let [c->a3 (<!! ca "3")]
                    (is (= "3" (get-label c->a3)))
                    (is (= "B or C" (get-content c->a3)))
                    (>!! ad mad)
                    (let [a->d (<!! ad "4")]
                      (is (= "4" (get-label a->d)))
                      (is (= "4d" (get-content a->d)))
                      (>!! da mad)
                      (let [d->a (<!! da "4")]
                        (is (= "4" (get-label d->a)))
                        (is (= "4d" (get-content d->a)))
                        (>!! [ab ac ad] m5)
                        (let [a->b5 (<!! ab "5")
                              a->c5 (<!! ac "5")
                              a->d5 (<!! ad "5")]
                          (is (= "5" (get-label a->b5)))
                          (is (= "bye all" (get-content a->b5)))
                          (is (= "5" (get-label a->c5)))
                          (is (= "bye all" (get-content a->c5)))
                          (is (= "5" (get-label a->d5)))
                          (is (= "bye all" (get-content a->d5)))))))))))))))

(deftest send-receive-single-recur-protocol
  (let [channels (generate-infrastructure (single-recur-protocol true))
        ab (get-channel "A" "B" channels)
        ba (get-channel "B" "A" channels)
        ac (get-channel "A" "C" channels)
        ca (get-channel "C" "A" channels)
        flag (atom false)]
    (set-logging-and-exceptions)
    (do (>!! ab (->message "1" "AB"))
        (let [a->b (<!! ab "1")]
          (is (= "1" (get-label a->b)))
          (is (= "AB" (get-content a->b)))
          (while (false? @flag)
            (>!! ba (->message "1" "AB"))
            (let [b->a (<!! ba "1")]
              (is (= "1" (get-label b->a)))
              (is (= "AB" (get-content b->a)))
              (if (== 1 (+ 1 (rand-int 2)))
                (do
                  (>!! ac (->message "2" "AC"))
                  (let [a->c (<!! ac "2")]
                    (is (= "2" (get-label a->c)))
                    (is (= "AC" (get-content a->c)))
                    (>!! ca (->message "2" "AC"))
                    (let [c->a (<!! ca "2")]
                      (is (= "2" (get-label c->a)))
                      (is (= "AC" (get-content c->a))))))
                (do
                  (>!! ab (->message "3" "AB3"))
                  (let [a->b3 (<!! ab "3")]
                    (is (= "3" (get-label a->b3)))
                    (is (= "AB3" (get-content a->b3)))
                    (reset! flag true))))))
          (>!! [ab ac] (->message "end" "ending"))
          (let [a->b-end (<!! ab "end")
                a->c-end (<!! ac "end")
                ]
            (is (= "end" (get-label a->b-end)))
            (is (= "ending" (get-content a->b-end)))
            (is (= "end" (get-label a->c-end)))
            (is (= "ending" (get-content a->c-end))))))))

(deftest send-receive-single-recur-wildcard-only-protocol
  (let [channels (generate-infrastructure (single-recur-protocol true))
        ab (get-channel "A" "B" channels)
        ba (get-channel "B" "A" channels)
        ac (get-channel "A" "C" channels)
        ca (get-channel "C" "A" channels)
        flag (atom false)]
    (do
      (enable-wildcard)
      (>!! ab (->message "1" "AB"))
        (let [a->b (<!! ab)]
          (is (= "1" (get-label a->b)))
          (is (= "AB" (get-content a->b)))
          (while (false? @flag)
            (>!! ba (->message "1" "AB"))
            (let [b->a (<!! ba)]
              (is (= "1" (get-label b->a)))
              (is (= "AB" (get-content b->a)))
              (if (== 1 (+ 1 (rand-int 2)))
                (do
                  (>!! ac (->message "2" "AC"))
                  (let [a->c (<!! ac)]
                    (is (= "2" (get-label a->c)))
                    (is (= "AC" (get-content a->c)))
                    (>!! ca (->message "2" "AC"))
                    (let [c->a (<!! ca)]
                      (is (= "2" (get-label c->a)))
                      (is (= "AC" (get-content c->a))))))
                (do
                  (>!! ab (->message "3" "AB3"))
                  (let [a->b3 (<!! ab)]
                    (is (= "3" (get-label a->b3)))
                    (is (= "AB3" (get-content a->b3)))
                    (reset! flag true))))))
          (>!! [ab ac] (->message "end" "ending"))
          (let [a->b-end (<!! ab)
                a->c-end (<!! ac)
                ]
            (is (= "end" (get-label a->b-end)))
            (is (= "ending" (get-content a->b-end)))
            (is (= "end" (get-label a->c-end)))
            (is (= "ending" (get-content a->c-end))))))))

(deftest send-receive-one-recur-with-choice-protocol
  (let [channels (generate-infrastructure (one-recur-with-choice-protocol true))
        ab (get-channel "A" "B" channels)
        ac (get-channel "A" "C" channels)
        flag (atom false)]
    (while (false? @flag)
      (if (== 1 (+ 1 (rand-int 2)))
        (do
          (>!! ac (->message "2" "AC"))
          (let [a->c (<!! ac "2")]
            (is (= "2" (get-label a->c)))
            (is (= "AC" (get-content a->c)))))
        (do
          (>!! ab (->message "3" "AB3"))
          (let [a->b3 (<!! ab "3")]
            (is (= "3" (get-label a->b3)))
            (is (= "AB3" (get-content a->b3)))
            (reset! flag true)))))))

(deftest send-receive-single-recur-one-choice-protocol
  (let [channels (generate-infrastructure (single-recur-one-choice-protocol))
        ab (get-channel "A" "B" channels)
        ba (get-channel "B" "A" channels)
        fnA (fn [fnA]
              (>!! ab (->message "1" {:threshold 5 :generatedNumber (rand-int (+ 10 10))}))
              (let [response (<!! ba ["2" "3"])]
                (cond
                  (= (get-label response) "2") (do
                                                 (println (format "greaterThan received with message: %s" (get-content response)))
                                                 (fnA fnA))
                  (= (get-label response) "3") (println (format "lessThan received with message: %s" (get-content response))))))
        fnB (fn [fnB]
              (let [numberMap (<!! ab "1")
                    threshold (:threshold (get-content numberMap))
                    generated (:generatedNumber (get-content numberMap))]
                (println numberMap)
                (if (> generated threshold)
                  (do (>!! ba (->message "2" "Number send is greater!"))
                      (fnB fnB))
                  (>!! ba (->message "3" "Number send is smaller!")))))

        ]
    (clojure.core.async/thread (fnA fnA))
    (clojure.core.async/thread (fnB fnB))
    ))

(deftest send-receive-one-recur-with-startchoice-and-endchoice-protocol
  (let [channels (generate-infrastructure (one-recur-with-startchoice-and-endchoice-protocol true))
        ab (get-channel "A" "B" channels)
        ac (get-channel "A" "C" channels)
        flag (atom false)]
    (set-logging-and-exceptions)
    (if (== 1 (+ 1 (rand-int 2)))
      (while (false? @flag)
        (if (== 1 (+ 1 (rand-int 2)))
          (do
            (println "repeating")
            (>!! ac (->message "2" "AC"))
            (let [a->c (<!! ac "2")]
              (is (= "2" (get-label a->c)))
              (is (= "AC" (get-content a->c)))))
          (do
            (println "took other branch")
            (>!! ab (->message "3" "AB3"))
            (println "send complete")
            (let [a->b3 (<!! ab "3")]
              (is (= "3" (get-label a->b3)))
              (is (= "AB3" (get-content a->b3)))
              (reset! flag true)))))
      (do
        (>!! ac (->message "2" "AC"))
        (let [a->c (<!! ac "2")]
          (is (= "2" (get-label a->c)))
          (is (= "AC" (get-content a->c))))))))

(deftest send-receive-dual-custom-channels-test
  (let [channels (generate-infrastructure (testDualProtocol true) [(generate-channel "A" "B" nil 3) (generate-channel "B" "A" nil 2)])
        ab (get-channel "A" "B" channels)
        ba (get-channel "B" "A" channels)
        m1 (->message "1" "Hello B")
        m2 (->message "2" "Hello A")]
    (is (== 3 (get-buffer ab)))
    (is (== 2 (get-buffer ba)))
    (do
      (>!! ab m1)
      (let [a->b (<!! ab "1")]
        (is (= "1" (get-label a->b)))
        (is (= "Hello B" (get-content a->b))))
      (>!! ba m2)
      (let [b->a (<!! ba "2")]
        (is (= "2" (get-label b->a)))
        (is (= "Hello A" (get-content b->a)))))))

(deftest send-receive-two-buyer-protocol-test
  (let [channels (generate-infrastructure (two-buyer-protocol))
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


(deftest send-receive-tesParallelParticipantsPrototocol
  (let [channels (add-infrastructure (tesParallelParticipantsProtocol))
        ab (get-channel "A" "B" channels)
        ac (get-channel "A" "C" channels)
        ba (get-channel "B" "A" channels)
        fnA (fn [] (do (>!! [ab ac] (msg "1" "Hi"))
                       (<!! ba "2")))
        fnB (fn [] (do (<!! ab "1")
                       (do (Thread/sleep 1)
                           (>!! ba (msg "2" "hi too")))))
        fnC (fn [] (<!! ac "1"))
        a (clojure.core.async/thread (fnA))
        c (clojure.core.async/thread (fnC))]
    (clojure.core.async/thread (fnB))
    (is (= "hi too" (get-content (async/<!! a))))
    (is (= "Hi") (get-content (async/<!! c)))))

(deftest send-receive-tesParallelParticipantsWithChoiceProtocol
  (let [channels (add-infrastructure (tesParallelParticipantsWithChoiceProtocol))
        ab (get-channel "A" "B" channels)
        ac (get-channel "A" "C" channels)
        ba (get-channel "B" "A" channels)
        fnA (fn [] (do
                     (>!! ab (msg "1" "hi"))
                     (let [b-a (<!! ba "2")]
                       (do (>!! [ab ac] (msg "3" "Hi"))
                           (get-content (<!! ba "4"))))))
        fnB (fn [] (let [a-b (<!! ab "1")]
                     (do
                       (>!! ba (msg "2" "hi too"))
                       (<!! ab "3")
                       (do (Thread/sleep 1)
                           (>!! ba (msg "4" "hi too"))))))
        fnC (fn [] (<!! ac "3"))
        a (clojure.core.async/thread (fnA))
        c (clojure.core.async/thread (fnC))
        ]
    (clojure.core.async/thread (fnB))
    (is (= "hi too"  (async/<!! a)))
    (is (= "Hi") (get-content (async/<!! c)))))