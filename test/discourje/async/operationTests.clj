(ns discourje.async.operationTests
  (:require [clojure.test :refer :all]
            [discourje.async.protocolTestData :refer :all]
            [discourje.core.async :refer :all]
            [clojure.core.async :as async]
            [discourje.core.logging :refer :all]))

(deftest add-sender
  (let [inter (->interaction nil 1 "a" "b" #{} nil)]
    (is (= (->interaction nil 1 "a" "b" #{"b"} nil) (assoc inter :accepted-sends (conj (:accepted-sends inter) "b"))))))

(deftest check-sender
  (let [inter (->interaction nil 1 "a" "b" #{"b"} nil)]
    (is (true? (contains? (get-accepted-sends inter) "b")))))

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

(deftest send-receive-multicast-protocol-test
  (let [channels (generate-infrastructure (testMulticastProtocol true))
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

(deftest send-receive-multicast-wildcard-only-protocol-test
  (let [channels (generate-infrastructure (testMulticastProtocol true))
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

(deftest send-receive-single-multicast-test
  (let [channels (generate-infrastructure (testSingleMulticastProtocol))
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
                                                 (fnA fnA))
                  (= (get-label response) "3") response)))
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
    (let [result-a (clojure.core.async/thread (fnA fnA))]
      (clojure.core.async/thread (fnB fnB))
      (is (= (get-label (async/<!! result-a)) "3")))))

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
            (>!! ac (->message "2" "AC"))
            (let [a->c (<!! ac "2")]
              (is (= "2" (get-label a->c)))
              (is (= "AC" (get-content a->c)))))
          (do
            (>!! ab (->message "3" "AB3"))
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
  (let [channels (generate-infrastructure (two-buyer-protocol true))
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


(deftest send-receive-testMulticastParticipantsPrototocol
  (let [channels (add-infrastructure (testMulticastParticipantsProtocol))
        ab (get-channel "A" "B" channels)
        ac (get-channel "A" "C" channels)
        ba (get-channel "B" "A" channels)
        fnA (fn [] (do (>!! [ab ac] (msg "1" "Hi"))
                       (<!! ba "2")))
        fnB (fn [] (do (<!!! ab "1")
                       (>!! ba (msg "2" "hi too"))))
        fnC (fn [] (<!!! ac "1"))
        a (clojure.core.async/thread (fnA))
        c (clojure.core.async/thread (fnC))]
    (clojure.core.async/thread (fnB))
    (is (= "hi too" (get-content (async/<!! a))))
    (is (= "Hi") (get-content (async/<!! c)))))

(deftest send-receive-testMulticastParticipantsWithChoiceProtocol
  (let [channels (add-infrastructure (testMulticastParticipantsWithChoiceProtocol))
        ab (get-channel "A" "B" channels)
        ac (get-channel "A" "C" channels)
        ba (get-channel "B" "A" channels)
        fnA (fn [] (do
                     (>!! ab (msg "1" "hi"))
                     (<!! ba "2")
                     (>!! [ab ac] (msg "3" "Hi"))
                     (get-content (<!!! ba "4"))))
        fnB (fn [] (do (<!! ab "1")
                       (>!! ba (msg "2" "hi too"))
                       (<!! ab "3")
                       (>!! ba (msg "4" "hi too"))))
        fnC (fn [] (<!! ac "3"))
        a (clojure.core.async/thread (fnA))
        c (clojure.core.async/thread (fnC))]
    (clojure.core.async/thread (fnB))
    (is (= "hi too" (async/<!! a)))
    (is (= "Hi") (get-content (async/<!! c)))))


(deftest send-and-receive-parallel-after-interaction-test
  (let [channels (add-infrastructure (parallel-after-interaction true))
        ab (get-channel "a" "b" channels)
        ba (get-channel "b" "a" channels)]
    (>!! ab (msg 1 1))
    (let [a->b (<!! ab 1)]
      (is (= (get-label a->b) 1))
      (do (>!! ba (msg 2 2))
          (let [b->a2 (<!! ba 2)]
            (is (= (get-label b->a2) 2))
            (>!! ab (msg 3 3))
            (is (= (get-label (<!! ab 3)) 3))))
      (do (>!! ba (msg 4 4))
          (let [b->a4 (<!! ba 4)]
            (is (= (get-label b->a4) 4))
            (>!! ab (msg 5 5))
            (is (= (get-label (<!! ab 5)) 5)))))))

(deftest send-and-receive-parallel-after-interaction-with-after-test
  (let [channels (add-infrastructure (parallel-after-interaction-with-after true))
        ab (get-channel "a" "b" channels)
        ba (get-channel "b" "a" channels)]
    (>!! ab (msg 1 1))
    (let [a->b (<!! ab 1)]
      (is (= (get-label a->b) 1))
      (do (>!! ba (msg 2 2))
          (let [b->a2 (<!! ba 2)]
            (is (= (get-label b->a2) 2))
            (>!! ab (msg 3 3))
            (is (= (get-label (<!! ab 3)) 3))))
      (do (>!! ba (msg 4 4))
          (let [b->a4 (<!! ba 4)]
            (is (= (get-label b->a4) 4))
            (>!! ab (msg 5 5))
            (is (= (get-label (<!! ab 5)) 5))))
      (do (>!! ba (msg 6 6))
          (let [b->a6 (<!! ba 6)]
            (is (= (get-label b->a6) 6)))))))

(deftest send-and-receive-parallel-after-interaction-with-after-test-THEADED
  (let [channels (add-infrastructure (parallel-after-interaction-with-after true))
        ab (get-channel "a" "b" channels)
        ba (get-channel "b" "a" channels)
        fn-first-parallel (fn [] (do (>!! ba (msg 2 2))
                                     (<!! ba 2)
                                     (>!! ab (msg 3 3))
                                     (get-label (<!! ab 3))))
        fn-second-parallel (fn [] (do (>!! ba (msg 4 4))
                                      (<!! ba 4)
                                      (>!! ab (msg 5 5))
                                      (get-label (<!! ab 5))))]
    (>!! ab (msg 1 1))
    (let [a->b (<!! ab 1)]
      (is (= (get-label a->b) 1))
      (let [fn1 (clojure.core.async/thread (fn-first-parallel))
            fn2 (clojure.core.async/thread (fn-second-parallel))]
        (is (= (async/<!! fn1) 3))
        (is (= (async/<!! fn2) 5))
        (do (>!! ba (msg 6 6))
            (let [b->a6 (<!! ba 6)]
              (is (= (get-label b->a6) 6))))))))


(deftest send-and-receive-parallel-after-choice-with-after-test
  (let [channels (add-infrastructure (parallel-after-choice-with-after true))
        ab (get-channel "a" "b" channels)
        ba (get-channel "b" "a" channels)]
    (>!! ab (msg 0 0))
    (let [a->b (<!! ab 0)]
      (is (= (get-label a->b) 0))
      (do (>!! ba (msg 2 2))
          (let [b->a2 (<!! ba 2)]
            (is (= (get-label b->a2) 2))
            (>!! ab (msg 3 3))
            (is (= (get-label (<!! ab 3)) 3))))
      (do (>!! ba (msg 4 4))
          (let [b->a4 (<!! ba 4)]
            (is (= (get-label b->a4) 4))
            (>!! ab (msg 5 5))
            (is (= (get-label (<!! ab 5)) 5))))
      (do (>!! ba (msg 6 6))
          (let [b->a6 (<!! ba 6)]
            (is (= (get-label b->a6) 6)))))))

(deftest send-and-receive-parallel-after-choice-with-after-choice-test
  (let [channels (add-infrastructure (parallel-after-choice-with-after-choice true))
        ab (get-channel "a" "b" channels)
        ba (get-channel "b" "a" channels)]
    (>!! ab (msg 0 0))
    (let [a->b (<!! ab 0)]
      (is (= (get-label a->b) 0))
      (do (>!! ba (msg 2 2))
          (let [b->a2 (<!! ba 2)]
            (is (= (get-label b->a2) 2))
            (>!! ab (msg 3 3))
            (is (= (get-label (<!! ab 3)) 3))))
      (do (>!! ba (msg 4 4))
          (let [b->a4 (<!! ba 4)]
            (is (= (get-label b->a4) 4))
            (>!! ab (msg 5 5))
            (is (= (get-label (<!! ab 5)) 5))))
      (do (>!! ba (msg 6 6))
          (let [b->a6 (<!! ba 6)]
            (is (= (get-label b->a6) 6)))))))

(deftest send-and-receive-parallel-after-rec-with-after-test
  (let [channels (add-infrastructure (parallel-after-rec-with-after true))
        ab (get-channel "a" "b" channels)
        ba (get-channel "b" "a" channels)]
    (>!! ab (msg 0 0))
    (let [a->b (<!! ab 0)]
      (is (= (get-label a->b) 0))
      (do (>!! ba (msg 2 2))
          (let [b->a2 (<!! ba 2)]
            (is (= (get-label b->a2) 2))
            (>!! ab (msg 3 3))
            (is (= (get-label (<!! ab 3)) 3))))
      (do (>!! ba (msg 4 4))
          (let [b->a4 (<!! ba 4)]
            (is (= (get-label b->a4) 4))
            (>!! ab (msg 5 5))
            (is (= (get-label (<!! ab 5)) 5))))
      (do (>!! ba (msg 6 6))
          (let [b->a6 (<!! ba 6)]
            (is (= (get-label b->a6) 6)))))))

(deftest send-and-receive-parallel-after-rec-with-after-rec-test
  (let [channels (add-infrastructure (parallel-after-rec-with-after-rec true))
        ab (get-channel "a" "b" channels)
        ba (get-channel "b" "a" channels)]
    (>!! ab (msg 0 0))
    (let [a->b (<!! ab 0)]
      (is (= (get-label a->b) 0))
      (do (>!! ba (msg 2 2))
          (let [b->a2 (<!! ba 2)]
            (is (= (get-label b->a2) 2))
            (>!! ab (msg 3 3))
            (is (= (get-label (<!! ab 3)) 3))))
      (do (>!! ba (msg 4 4))
          (let [b->a4 (<!! ba 4)]
            (is (= (get-label b->a4) 4))
            (>!! ab (msg 5 5))
            (is (= (get-label (<!! ab 5)) 5))))
      (do (>!! ba (msg 7 7))
          (let [b->a6 (<!! ba 7)]
            (is (= (get-label b->a6) 7)))))))

(deftest send-and-receive-parallel-after-rec-with-after-rec-with-recur-test
  (let [channels (add-infrastructure (parallel-after-rec-with-after-rec true))
        ab (get-channel "a" "b" channels)
        ba (get-channel "b" "a" channels)]
    (>!! ab (msg 0 0))
    (let [a->b (<!! ab 0)]
      (is (= (get-label a->b) 0))
      (do (>!! ba (msg 2 2))
          (let [b->a2 (<!! ba 2)]
            (is (= (get-label b->a2) 2))
            (>!! ab (msg 3 3))
            (is (= (get-label (<!! ab 3)) 3))))
      (do (>!! ba (msg 4 4))
          (let [b->a4 (<!! ba 4)]
            (is (= (get-label b->a4) 4))
            (>!! ab (msg 5 5))
            (is (= (get-label (<!! ab 5)) 5))))
      (do (>!! ba (msg 6 6))
          (let [b->a6 (<!! ba 6)]
            (is (= (get-label b->a6) 6))))
      (do (>!! ba (msg 7 7))
          (let [b->a7 (<!! ba 7)]
            (is (= (get-label b->a7) 7)))))))

(deftest send-and-receive-nested-parallel-test
  (let [channels (add-infrastructure (nested-parallel true))
        ab (get-channel "a" "b" channels)
        ba (get-channel "b" "a" channels)]
    (>!! ab (msg 1 1))
    (let [a->b (<!! ab 1)]
      (is (= (get-label a->b) 1))
      (do (>!! ba (msg 2 2))
          (let [b->a2 (<!! ba 2)]
            (is (= (get-label b->a2) 2))
            (>!! ab (msg 3 3))
            (is (= (get-label (<!! ab 3)) 3)))
          (>!! ba (msg 4 4))
          (let [b->a4 (<!! ba 4)]
            (is (= (get-label b->a4) 4))
            (>!! ab (msg 5 5))
            (is (= (get-label (<!! ab 5)) 5))))
      (do (>!! ba (msg "a" "a"))
          (let [b->aA (<!! ba "a")]
            (is (= (get-label b->aA) "a"))
            (>!! ab (msg "b" "b"))
            (is (= (get-label (<!! ab "b")) "b")))
          (>!! ba (msg "b" "a"))
          (let [b->aB (<!! ba "b")]
            (is (= (get-label b->aB) "b"))
            (>!! ab (msg "a" "a"))
            (is (= (get-label (<!! ab "a")) "a")))))))

(deftest send-and-receive-nested-parallel-Threaded-test
  (let [channels (add-infrastructure (nested-parallel true))
        ab (get-channel "a" "b" channels)
        ba (get-channel "b" "a" channels)
        fn-par-00 (fn []
                    (>!! ba (msg "a" "a"))
                    (<!! ba "a")
                    (>!! ab (msg "b" "b"))
                    (<!! ab "b"))
        fn-par-01 (fn []
                    (>!! ba (msg "b" "a"))
                    (<!! ba "b")
                    (>!! ab (msg "a" "a"))
                    (<!! ab "a"))
        fn-par-10 (fn []
                    (>!! ba (msg 2 2))
                    (<!! ba 2)
                    (>!! ab (msg 3 3))
                    (<!! ab 3))
        fn-par-11 (fn []
                    (>!! ba (msg 4 4))
                    (<!! ba 4)
                    (>!! ab (msg 5 5))
                    (<!! ab 5))
        ]
    (>!! ab (msg 1 1))
    (let [a->b (<!! ab 1)
          f00 (async/thread (fn-par-00))
          f01 (async/thread (fn-par-01))
          f10 (async/thread (fn-par-10))
          f11 (async/thread (fn-par-11))]
      (is (= (get-label a->b) 1))
      (is (= (get-label (async/<!! f00)) "b"))
      (is (= (get-label (async/<!! f01)) "a"))
      (is (= (get-label (async/<!! f10)) 3))
      (is (= (get-label (async/<!! f11)) 5)))))

(deftest send-and-receive-after-parallel-nested-parallel-test
  (let [channels (add-infrastructure (after-parallel-nested-parallel true))
        ab (get-channel "a" "b" channels)
        ba (get-channel "b" "a" channels)]
    (do
      (>!! ba (msg 0 0))
      (is (= (get-label (<!! ba 0)) 0))
      (>!! ab (msg 1 1))
      (is (= (get-label (<!! ab 1)) 1)))
    (do
      (>!! ba (msg "hi" "hi"))
      (is (= (get-label (<!! ba "hi")) "hi"))
      (>!! ab (msg "hi" "hi"))
      (is (= (get-label (<!! ab "hi")) "hi")))
    (do (>!! ba (msg 2 2))
        (let [b->a2 (<!! ba 2)]
          (is (= (get-label b->a2) 2))
          (>!! ab (msg 3 3))
          (is (= (get-label (<!! ab 3)) 3)))
        (>!! ba (msg 4 4))
        (let [b->a4 (<!! ba 4)]
          (is (= (get-label b->a4) 4))
          (>!! ab (msg 5 5))
          (is (= (get-label (<!! ab 5)) 5))))
    (do (>!! ba (msg "a" "a"))
        (let [b->aA (<!! ba "a")]
          (is (= (get-label b->aA) "a"))
          (>!! ab (msg "b" "b"))
          (is (= (get-label (<!! ab "b")) "b")))
        (>!! ba (msg "b" "a"))
        (let [b->aB (<!! ba "b")]
          (is (= (get-label b->aB) "b"))
          (>!! ab (msg "a" "a"))
          (is (= (get-label (<!! ab "a")) "a"))))))

(deftest send-and-receive-after-parallel-nested-parallel-Threaded-test
  (let [channels (add-infrastructure (after-parallel-nested-parallel true))
        ab (get-channel "a" "b" channels)
        ba (get-channel "b" "a" channels)
        fn-par0 (fn []
                  (>!! ba (msg 0 0))
                  (<!! ba 0)
                  (>!! ab (msg 1 1))
                  (<!! ab 1))
        fn-par1 (fn []
                  (>!! ba (msg "hi" "hi"))
                  (<!! ba "hi")
                  (>!! ab (msg "hi" "hi"))
                  (<!! ab "hi"))
        fn-par-00 (fn []
                    (>!! ba (msg "a" "a"))
                    (<!! ba "a")
                    (>!! ab (msg "b" "b"))
                    (<!! ab "b"))
        fn-par-01 (fn []
                    (>!! ba (msg "b" "a"))
                    (<!! ba "b")
                    (>!! ab (msg "a" "a"))
                    (<!! ab "a"))
        fn-par-10 (fn []
                    (>!! ba (msg 2 2))
                    (<!! ba 2)
                    (>!! ab (msg 3 3))
                    (<!! ab 3))
        fn-par-11 (fn []
                    (>!! ba (msg 4 4))
                    (<!! ba 4)
                    (>!! ab (msg 5 5))
                    (<!! ab 5))
        ]
    (let [f0 (async/thread (fn-par0))
          f1 (async/thread (fn-par1))]
      (is (= (get-label (async/<!! f0)) 1))
      (is (= (get-label (async/<!! f1)) "hi"))
      (let [f00 (async/thread (fn-par-00))
            f01 (async/thread (fn-par-01))
            f10 (async/thread (fn-par-10))
            f11 (async/thread (fn-par-11))]
        (is (= (get-label (async/<!! f00)) "b"))
        (is (= (get-label (async/<!! f01)) "a"))
        (is (= (get-label (async/<!! f10)) 3))
        (is (= (get-label (async/<!! f11)) 5))))))

(deftest impossible-parallel-test
  "This test shows an issue with parallel when both parallel branches are going to the same channel (bob-to-alice and alice to bob)
  So both sends are allowed by the monitor, but the receives could be not allowed since forexample: on the channel the order of messages in the QUEUE is
  (msg 2 2) followed by (msg 3 3) but the receive for 3 is done first. This will create a faulty receive since the 2 message is the first message in the queue of the channel!
  This test handles it by exceptions"
  (let [mon (generate-infrastructure (mep (-->> 1 "Alice" "Bob")
                                          (par [(-->> 2 "Bob" "Alice")
                                                (-->> 3 "Alice" "Bob")]
                                               [(-->> 4 "Bob" "Alice")
                                                (-->> 5 "Alice" "Bob")])))
        alice-to-bob (get-channel "Alice" "Bob" mon)
        bob-to-alice (get-channel "Bob" "Alice" mon)
        alice (fn []
                (>!! alice-to-bob (msg 1 1))
                (let [first-par (fn []
                                  (<!! bob-to-alice 2)
                                  (>!! alice-to-bob (msg 3 3)))
                      second-par (fn []
                                   (<!! bob-to-alice 4)
                                   (>!! alice-to-bob (msg 5 5)))]
                  (clojure.core.async/thread (first-par))
                  (clojure.core.async/thread (second-par))))
        bob (fn []
              (<!! alice-to-bob 1)
              (let [first-par (fn []
                                (>!! bob-to-alice (msg 2 2))
                                (<!! alice-to-bob 3))
                    second-par (fn []
                                 (>!! bob-to-alice (msg 4 4))
                                 (<!! alice-to-bob 5))]
                (clojure.core.async/thread (first-par))
                (clojure.core.async/thread (second-par))))]
    (alice)
    (loop [try-test (get-label (async/<!! (bob)))]
      (if-not (nil? try-test)
        (is (= try-test 5))
        (recur (get-label (async/<!! (bob))))))))

(deftest send-and-receive-parallel-with-choice-test
  (let [channels (add-infrastructure (parallel-with-choice true))
        ab (get-channel "a" "b" channels)
        ba (get-channel "b" "a" channels)]
    (set-logging-exceptions)
    ;only when sending validation for par, we need to keep track of nesting
    (>!! ab (msg 0 0))
    (let [a->b (<!! ab 0)]
      (is (= (get-label a->b) 0))
      (do (>!! ba (msg 4 4))
          (let [b->a4 (<!! ba 4)]
            (is (= (get-label b->a4) 4))
            (>!! ab (msg 5 5))
            (is (= (get-label (<!! ab 5)) 5))))
      (do (>!! ba (msg 6 6))
          (let [b->a6 (<!! ba 6)]
            (is (= (get-label b->a6) 6))
            (is (nil? (get-active-interaction (get-monitor ab)))))))))

(deftest send-and-receive-parallel-with-choice-with-parallel-test
  (let [channels (add-infrastructure (parallel-with-choice-with-parallel true))
        ab (get-channel "a" "b" channels)
        ba (get-channel "b" "a" channels)]
    (set-logging-exceptions)
    (>!! ab (msg 0 0))
    (let [a->b (<!! ab 0)]
      (is (= (get-label a->b) 0))
      (do (>!! ba (msg 4 4))
          (let [b->a4 (<!! ba 4)]
            (is (= (get-label b->a4) 4))
            (>!! ab (msg 5 5))
            (is (= (get-label (<!! ab 5)) 5))))
      (do (>!! ba (msg "hi" "hi"))
          (let [b->ahi (<!! ba "hi")]
            (is (= (get-label b->ahi) "hi"))
            (>!! ab (msg "hi" "hi"))
            (is (= (get-label (<!! ab "hi")) "hi"))))
      (do (>!! ba (msg 6 6))
          (let [b->a6 (<!! ba 6)]
            (is (= (get-label b->a6) 6))
            (is (nil? (get-active-interaction (get-monitor ab)))))))))

(deftest send-and-receive-parallel-with-rec-test
  (let [channels (add-infrastructure (parallel-with-rec true))
        ab (get-channel "a" "b" channels)
        ba (get-channel "b" "a" channels)]
    (set-logging-exceptions)
    (loop [reps 0]
      (if (> reps 2)
        (do (>!! ab (msg 1 1))
            (is (= (get-label (<!! ab 1)) 1)))
        (do (>!! ab (msg 0 0))
            (println reps)
            (println "active monitor" (get-active-interaction (get-monitor ab)))
            (is (= (get-label (<!! ab 0)) 0))
            (recur (+ reps 1)))))
    (do (>!! ba (msg 4 4))
        (let [b->a4 (<!! ba 4)]
          (is (= (get-label b->a4) 4))
          (>!! ab (msg 5 5))
          (is (= (get-label (<!! ab 5)) 5))))
    (do (>!! ba (msg 6 6))
        (let [b->a6 (<!! ba 6)]
          (is (= (get-label b->a6) 6))
          (is (nil? (get-active-interaction (get-monitor ab))))))))
