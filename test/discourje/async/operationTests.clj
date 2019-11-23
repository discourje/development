(ns discourje.async.operationTests
  (:require [clojure.test :refer :all]
            [discourje.async.protocolTestData :refer :all ]
            [discourje.core.async :refer :all]
            [clojure.core.async :as async]
            [discourje.core.logging :refer :all]))


(defn <!!-test
  "Utility method to fix all test cases"
  [channel]
  (let [value (discourje.core.async/<!! channel)]
    (get-content value)))

(defn <!!!-test
  "Utility method to fix all test cases"
  [channel]
  (let [value (discourje.core.async/<!!! channel)]
    (get-content value)))

(deftest add-sender
  (let [inter (->interaction nil 1 "a" "b" #{} nil)]
    (is (= (->interaction nil 1 "a" "b" #{"b"} nil) (assoc inter :accepted-sends (conj (:accepted-sends inter) "b"))))))

(deftest check-sender
  (let [inter (->interaction nil 1 "a" "b" #{"b"} nil)]
    (is (true? (contains? (get-accepted-sends inter) "b")))))

(deftest send-test
  (let [channels (generate-infrastructure (testDualProtocol true))
        c (get-channel channels "A" "B")]
    (>!! c (msg "1" "hello world"))
    (let [m (async/<!! (get-chan c))]
      (is (= "1" (get-label m)))
      (is (= "hello world" (get-content m))))))

(deftest send-receive-dual-test
  (let [channels (generate-infrastructure (testDualProtocol true))
        ab (get-channel channels "A" "B")
        ba (get-channel channels "B" "A")
        m1 (->message "1" "Hello B")
        m2 (->message "2" "Hello A")]
    (set-logging)
    (do
      (>!! ab m1)
      (let [a->b (<!!-test ab)]
        (is (= "Hello B" a->b)))
      (>!! ba m2)
      (let [b->a (<!!-test ba)]
        (is (= "Hello A" b->a))))))

(deftest send-receive-typed-dual-test
  (let [channels (generate-infrastructure (test-typed-DualProtocol true))
        ab (get-channel channels "A" "B")
        ba (get-channel channels "B" "A")]
    (do
      (>!! ab "Hello B")
      (let [a->b (<!! ab)]
        (is (= "Hello B" a->b)))
      (>!! ba "Hello A")
      (let [b->a (<!! ba)]
        (is (= "Hello A" b->a))))))

(deftest send-receive-multicast-protocol-test
  (let [channels (generate-infrastructure (testMulticastProtocol true))
        ab (get-channel channels "A" "B")
        ba (get-channel channels "B" "A")
        ac (get-channel channels "A" "C")
        ca (get-channel channels "C" "A")
        cb (get-channel channels "C" "B")]
    (do
      (>!! ab (->message "1" "A->B"))
      (let [a->b (<!!-test ab)]
        (is (= "A->B" a->b))
        (>!! ba (->message "2" "B->A"))
        (let [b->a (<!!-test ba)]
          (is (= "B->A" b->a))
          (>!! ac (->message "3" "A->C"))
          (let [a->c (<!!-test ac)]
            (is (= "A->C" a->c))
            (>!! [ca cb] (->message "4" "C->A-B"))
            (let [c->a (<!!-test ca)
                  c->b (<!!-test cb)]
              (is (= "C->A-B" c->a))
              (is (= "C->A-B" c->b))
              (is (nil? (get-active-interaction (get-monitor ab)))))))))))

(deftest send-receive-multicast-wildcard-only-protocol-test
  (let [channels (generate-infrastructure (testMulticastProtocol true))
        ab (get-channel channels "A" "B")
        ba (get-channel channels "B" "A")
        ac (get-channel channels "A" "C")
        ca (get-channel channels "C" "A")
        cb (get-channel channels "C" "B")]
    (do
      (>!! ab (->message "1" "A->B"))
      (let [a->b (<!!-test ab)]
        (is (= "A->B" a->b))
        (>!! ba (->message "2" "B->A"))
        (let [b->a (<!!-test ba)]
          (is (= "B->A" b->a))
          (>!! ac (->message "3" "A->C"))
          (let [a->c (<!!-test ac)]
            (is (= "A->C" a->c))
            (>!! [ca cb] (->message "4" "C->A-B"))
            (let [c->a (<!!-test ca)
                  c->b (<!!-test cb)]
              (is (= "C->A-B" c->a))
              (is (= "C->A-B" c->b)))))))))

(deftest send-receive-single-multicast-test
  (let [channels (generate-infrastructure (testSingleMulticastProtocol))
        ab (get-channel channels "A" "B")
        ac (get-channel channels "A" "C")
        m1 (->message "1" "Hello B and C")]
    (do
      (>!! [ab ac] m1)
      (let [a->b (<!!-test ab)
            a->c (<!!-test ac)]
        (is (= "Hello B and C" a->b))
        (is (= a->c a->b))
        (is (nil? (get-active-interaction (get-monitor ab))))))))

(deftest send-receive-single-Always0-choice-protocol
  (let [channels (generate-infrastructure (single-choice-protocol))
        ab (get-channel channels "A" "B")
        ma (->message "1" "Hello B")]
    (do
      (>!! ab ma)
      (let [a->b (<!!-test ab)]
        (is (= "Hello B" a->b))))))

(deftest send-receive-single-always1-choice-protocol
  (let [channels (generate-infrastructure (single-choice-protocol))
        ac (get-channel channels "A" "C")
        mc (->message "hi" "Hi C")]
    (do
      (>!! ac mc)
      (let [a->c (<!!-test ac)]
        (is (= "Hi C" a->c))))))

(deftest send-receive-single-Random-choice-protocol
  (let [channels (generate-infrastructure (single-choice-protocol))
        ab (get-channel channels "A" "B")
        ac (get-channel channels "A" "C")
        ma (->message "1" "Hello B")
        mc (->message "hi" "Hi C")]
    (if (== 0 (rand-int 2))
      (do
        (>!! ab ma)
        (let [a->b (<!!-test ab)]
          (is (= "Hello B" a->b))))
      (do
        (>!! ac mc)
        (let [a->c (<!!-test ac)]
          (is (= "Hi C" a->c)))))))

(deftest send-receive-multiple-nested-choice-branch-protocol
  (let [channels (generate-infrastructure (multiple-nested-choice-branch-protocol true))
        ab (get-channel channels "A" "B")
        n (+ 1 (rand-int 4))]
    (cond
      (== n 1)
      (do (>!! ab (->message "1" "AB"))
          (let [a->b (<!!-test ab)]
            (is (= "AB" a->b))))
      (== n 2)
      (do (>!! ab (->message "2" "AB"))
          (let [a->b (<!!-test ab)]
            (is (= "AB" a->b))))
      (== n 3)
      (do (>!! ab (->message "3" "AB"))
          (let [a->b (<!!-test ab )]
            (is (= "AB" a->b))))
      (== n 4)
      (do (>!! ab (->message "4" "AB"))
          (let [a->b (<!!-test ab)]
            (is (= "AB" a->b)))))))


(deftest send-receive-single-choice-in-middle-always0-choice-protocol
  (let [channels (generate-infrastructure (single-choice-in-middle-protocol true))
        sf (get-channel channels "Start" "Finish")
        fs (get-channel channels "Finish" "Start")
        ab (get-channel channels "A" "B")
        ba (get-channel channels "B" "A")
        msf (->message "99" "Starting!")
        mab (->message "1" "1B")
        mba (->message "bla" "blaA")
        mfs (->message "88" "ending!")]
    (set-logging-and-exceptions)
    (do
      (>!! sf msf)
      (let [s->f (<!!-test sf)]
        (is (= "Starting!" s->f))
        (>!! ab mab)
        (let [a->b (<!!-test ab)]
          (is (= "1B" a->b))
          (>!! ba mba)
          (let [b->a (<!!-test ba)]
            (is (= "blaA" b->a))
            (>!! fs mfs)
            (let [f->s (<!!-test fs)]
              (is (= "ending!" f->s))))
          )))))

(deftest send-receive-single-choice-multiple-interactions-protocol-test
  (let [channels (generate-infrastructure (single-choice-multiple-interactions-protocol true))
        ab (get-channel channels "A" "B")
        ba (get-channel channels "B" "A")
        ac (get-channel channels "A" "C")
        ca (get-channel channels "C" "A")
        ad (get-channel channels "A" "D")
        da (get-channel channels "D" "A")
        mab1 (->message "1" "1ab")
        mab-c2 (->message "2" "B or C")
        mab-c3 (->message "3" "B or C")
        mad (->message "4" "4d")
        m5 (->message "5" "bye all")]
    (do (>!! ab mab1)
        (let [a->b (<!!-test ab)]
          (is (= "1ab" a->b))
          (>!! ba mab1)
          (let [b->a (<!!-test ba)]
            (is (= "1ab" b->a))
            (>!! ac mab-c2)
            (let [a->c (<!!-test ac)]
              (is (= "B or C" a->c))
              (>!! ca mab-c2)
              (let [c->a (<!!-test ca)]
                (is (= "B or C" c->a))
                (>!! ac mab-c3)
                (let [a->c3 (<!!-test ac)]
                  (is (= "B or C" a->c3))
                  (>!! ca mab-c3)
                  (let [c->a3 (<!!-test ca)]
                    (is (= "B or C" c->a3))
                    (>!! ad mad)
                    (let [a->d (<!!-test ad)]
                      (is (= "4d" a->d))
                      (>!! da mad)
                      (let [d->a (<!!-test da)]
                        (is (= "4d" d->a))
                        (>!! [ab ac ad] m5)
                        (let [a->b5 (<!!-test ab)
                              a->c5 (<!!-test ac)
                              a->d5 (<!!-test ad)]
                          (is (= "bye all" a->b5))
                          (is (= "bye all" a->c5))
                          (is (= "bye all" a->d5))
                          (is (nil? (get-active-interaction (get-monitor ab))))))))))))))))

(deftest send-receive-single-recur-protocol
  (let [channels (generate-infrastructure (single-recur-protocol true))
        ab (get-channel channels "A" "B")
        ba (get-channel channels "B" "A")
        ac (get-channel channels "A" "C")
        ca (get-channel channels "C" "A")
        flag (atom false)]
    (set-logging-and-exceptions)
    (do (>!! ab (->message "1" "AB"))
        (let [a->b (<!!-test ab)]
          (is (= "AB" a->b))
          (while (false? @flag)
            (>!! ba (->message "1" "AB"))
            (let [b->a (<!!-test ba)]
              (is (= "AB" b->a))
              (if (== 1 (+ 1 (rand-int 2)))
                (do
                  (>!! ac (->message "2" "AC"))
                  (let [a->c (<!!-test ac)]
                    (is (= "AC" a->c))
                    (>!! ca (->message "2" "AC"))
                    (let [c->a (<!!-test ca)]
                      (is (= "AC" c->a)))))
                (do
                  (>!! ab (->message "3" "AB3"))
                  (let [a->b3 (<!!-test ab)]
                    (is (= "AB3" a->b3))
                    (reset! flag true))))))
          (>!! [ab ac] (->message "end" "ending"))
          (let [a->b-end (<!!-test ab)
                a->c-end (<!!-test ac)]
            (is (= "ending" a->b-end))
            (is (= "ending" a->c-end)))))))

(deftest send-receive-single-recur-wildcard-only-protocol
  (let [channels (generate-infrastructure (single-recur-protocol true))
        ab (get-channel channels "A" "B")
        ba (get-channel channels "B" "A")
        ac (get-channel channels "A" "C")
        ca (get-channel channels "C" "A")
        flag (atom false)]
    (do
      (>!! ab (->message "1" "AB"))
      (let [a->b (<!!-test ab)]
        (is (= "AB" a->b))
        (while (false? @flag)
          (>!! ba (->message "1" "AB"))
          (let [b->a (<!!-test ba)]
            (is (= "AB" b->a))
            (if (== 1 (+ 1 (rand-int 2)))
              (do
                (>!! ac (->message "2" "AC"))
                (let [a->c (<!!-test ac)]
                  (is (= "AC" a->c))
                  (>!! ca (->message "2" "AC"))
                  (let [c->a (<!!-test ca)]
                    (is (= "AC" c->a)))))
              (do
                (>!! ab (->message "3" "AB3"))
                (let [a->b3 (<!!-test ab)]
                  (is (= "AB3" a->b3))
                  (reset! flag true))))))
        (>!! [ab ac] (->message "end" "ending"))
        (let [a->b-end (<!!-test ab)
              a->c-end (<!!-test ac)]
          (is (= "ending" a->b-end))
          (is (= "ending" a->c-end)))))))

(deftest send-receive-one-recur-with-choice-protocol
  (let [channels (generate-infrastructure (one-recur-with-choice-protocol true))
        ab (get-channel channels "A" "B")
        ac (get-channel channels "A" "C")
        flag (atom false)]
    (while (false? @flag)
      (if (== 1 (+ 1 (rand-int 2)))
        (do
          (>!! ac (->message "2" "AC"))
          (let [a->c (<!!-test ac)]
            (is (= "AC" a->c))))
        (do
          (>!! ab (->message "3" "AB3"))
          (let [a->b3 (<!!-test ab)]
            (is (= "AB3" a->b3))
            (reset! flag true)))))))

(deftest send-receive-single-recur-one-choice-protocol
  (let [channels (generate-infrastructure (single-recur-one-choice-protocol))
        ab (get-channel channels "A" "B")
        ba (get-channel channels "B" "A")
        fnA (fn [fnA]
              (>!! ab (->message "1" {:threshold 5 :generatedNumber 2}))
              (let [response (<!!-test ba)]
                (cond
                  (= (:label response) "2") (do (fnA fnA))
                  (= (:label response) "3") response)))
        fnB (fn [fnB]
              (let [numberMap (<!!-test ab)
                    threshold (:threshold numberMap)
                    generated (:generatedNumber numberMap)]
                (if (> generated threshold)
                  (do (>!! ba (->message "2" {:label "2" :content "Number send is greater!"}))
                      (fnB fnB))
                  (>!! ba (->message "3" {:label "3" :content "Number send is smaller!"})))))

        ]
    (let [result-a (clojure.core.async/thread (fnA fnA))]
      (clojure.core.async/thread (fnB fnB))
      (is (= (:label (async/<!! result-a)) "3"))
      (is (nil? (get-active-interaction (get-monitor ab)))))))

(deftest send-receive-one-recur-with-startchoice-and-endchoice-protocol
  (let [channels (generate-infrastructure (one-recur-with-startchoice-and-endchoice-protocol true))
        ab (get-channel channels "A" "B")
        ac (get-channel channels "A" "C")
        flag (atom false)]
    (set-logging-and-exceptions)
    (if (== 1 (+ 1 (rand-int 2)))
      (while (false? @flag)
        (if (== 1 (+ 1 (rand-int 2)))
          (do
            (>!! ac (->message "2" "AC"))
            (let [a->c (<!!-test ac)]
              (is (= "AC" a->c))))
          (do
            (>!! ab (->message "3" "AB3"))
            (let [a->b3 (<!!-test ab)]
              (is (= "AB3" a->b3))
              (reset! flag true)))))
      (do
        (>!! ac (->message "2" "AC"))
        (let [a->c (<!!-test ac)]
          (is (= "AC" a->c)))))))

(deftest send-receive-dual-custom-channels-test
  (let [channels (generate-infrastructure (testDualProtocol true) [(generate-channel "A" "B" nil 3) (generate-channel "B" "A" nil 2)])
        ab (get-channel channels "A" "B")
        ba (get-channel channels "B" "A")
        m1 (->message "1" "Hello B")
        m2 (->message "2" "Hello A")]
    (is (== 3 (get-buffer ab)))
    (is (== 2 (get-buffer ba)))
    (do
      (>!! ab m1)
      (let [a->b (<!!-test ab)]
        (is (= "Hello B" a->b)))
      (>!! ba m2)
      (let [b->a (<!!-test ba)]
        (is (= "Hello A" b->a))))))

(deftest send-receive-two-buyer-protocol-test
  (let [channels (generate-infrastructure (two-buyer-protocol true))
        b1s (get-channel channels "Buyer1" "Seller")
        sb1 (get-channel channels "Seller" "Buyer1")
        sb2 (get-channel channels "Seller" "Buyer2")
        b1b2 (get-channel channels "Buyer1" "Buyer2")
        b2s (get-channel channels "Buyer2" "Seller")
        order-book (atom true)]
    (while (true? @order-book)
      (do
        (>!! b1s (->message "title" "The Joy of Clojure"))
        (let [b1-title-s (<!!-test b1s)]
          (is (= "The Joy of Clojure"  b1-title-s))
          (>!! [sb1 sb2] (->message "quote" (+ 1 (rand-int 20))))
          (let [s-quote-b1 (<!!-test sb1)
                s-quote-b2 (<!!-test sb2)]
            (>!! b1b2 (->message "quoteDiv" (rand-int  s-quote-b1)))
            (let [b1-quoteDiv-b2 (<!!-test b1b2)]
              (if (>= (* 100 (float (/  b1-quoteDiv-b2  s-quote-b2))) 50)
                (do
                  (>!! b2s (->message "ok" "Open University, Valkenburgerweg 177, 6419 AT, Heerlen"))
                  (let [b2-ok-s (<!!-test b2s)]
                    (is (= "Open University, Valkenburgerweg 177, 6419 AT, Heerlen"  b2-ok-s))
                    (>!! sb2 (->message "date" "09-04-2019"))
                    (let [s-date-b2 (<!!-test sb2)]
                      (is (= "09-04-2019"  s-date-b2)))))
                (do
                  (>!! b2s (->message "quit" "Price to high"))
                  (let [b2-quit-s (<!!-test b2s)]
                    (is (= "Price to high"  b2-quit-s))
                    (reset! order-book false)))))))))))


(deftest send-receive-testMulticastParticipantsPrototocol
  (let [channels (add-infrastructure (testMulticastParticipantsProtocol))
        ab (get-channel channels "A" "B")
        ac (get-channel channels "A" "C")
        ba (get-channel channels "B" "A")
        fnA (fn [] (do (>!! [ab ac] (msg "1" 1))
                       (<!!-test ba)))
        fnB (fn [] (do (<!!!-test ab)
                       (>!! ba (msg "2" 2))))
        fnC (fn [] (<!!!-test ac))
        a (clojure.core.async/thread (fnA))
        c (clojure.core.async/thread (fnC))]
    (clojure.core.async/thread (fnB))
    (is (= 1 (async/<!! c)))
    (is (= 2 (async/<!! a)))
    (is (nil? (get-active-interaction (get-monitor ab))))))

(deftest send-receive-testMulticastParticipantsWithChoiceProtocol
  (let [channels (add-infrastructure (testMulticastParticipantsWithChoiceProtocol))
        ab (get-channel channels "A" "B")
        ac (get-channel channels "A" "C")
        ba (get-channel channels "B" "A")
        fnA (fn [] (do
                     (>!! ab (msg "1" 1))
                     (<!!-test ba)
                     (>!! [ab ac] (msg "3" 3))
                     (<!!!-test ba)))
        fnB (fn [] (do (<!!-test ab)
                       (>!! ba (msg "2" 2))
                       (<!!!-test ab)
                       (>!! ba (msg "4" 4))))
        fnC (fn [] (<!!!-test ac))
        a (clojure.core.async/thread (fnA))
        c (clojure.core.async/thread (fnC))]
    (clojure.core.async/thread (fnB))
    (is (= 4 (async/<!! a)))
    (is (= 3 (async/<!! c)))
    (is (nil? (get-active-interaction (get-monitor ab))))))

(deftest send-and-receive-parallel-after-interaction-test
  (let [channels (add-infrastructure (parallel-after-interaction true))
        ab (get-channel channels "a" "b")
        ba (get-channel channels "b" "a")]
    (>!! ab (msg 1 1))
    (let [a->b (<!!-test ab)]
      (is (=  a->b 1))
      (do (>!! ba (msg 2 2))
          (let [b->a2 (<!!-test ba)]
            (is (=  b->a2 2))
            (>!! ab (msg 3 3))
            (is (= (<!!-test ab) 3))))
      (do (>!! ba (msg 4 4))
          (let [b->a4 (<!!-test ba)]
            (is (=  b->a4 4))
            (>!! ab (msg 5 5))
            (is (= (<!!-test ab) 5)))))))

(deftest send-and-receive-parallel-after-interaction-with-after-test
  (let [channels (add-infrastructure (parallel-after-interaction-with-after true))
        ab (get-channel channels "a" "b")
        ba (get-channel channels "b" "a")]
    (>!! ab (msg 1 1))
    (let [a->b (<!!-test ab)]
      (is (=  a->b 1))
      (do (>!! ba (msg 2 2))
          (let [b->a2 (<!!-test ba)]
            (is (=  b->a2 2))
            (>!! ab (msg 3 3))
            (is (= (<!!-test ab) 3))))
      (do (>!! ba (msg 4 4))
          (let [b->a4 (<!!-test ba)]
            (is (=  b->a4 4))
            (>!! ab (msg 5 5))
            (is (= (<!!-test ab) 5))))
      (do (>!! ba (msg 6 6))
          (let [b->a6 (<!!-test ba)]
            (is (=  b->a6 6)))))))

(deftest send-and-receive-parallel-after-interaction-with-after-test-THREADED
  (let [channels (add-infrastructure (parallel-after-interaction-with-after true))
        ab (get-channel channels "a" "b")
        ba (get-channel channels "b" "a")
        fn-first-parallel (fn [] (do (>!! ba (msg 2 2))
                                     (<!!-test ba)
                                     (>!! ab (msg 3 3))
                                     (<!!-test ab)))
        fn-second-parallel (fn [] (do (>!! ba (msg 4 4))
                                      (<!!-test ba)
                                      (>!! ab (msg 5 5))
                                      (<!!-test ab)))]
    (>!! ab (msg 1 1))
    (let [a->b (<!!-test ab)]
      (is (=  a->b 1))
      (let [fn1 (clojure.core.async/thread (fn-first-parallel))
            fn2 (clojure.core.async/thread (fn-second-parallel))]
        (is (= (async/<!! fn1) 3))
        (is (= (async/<!! fn2) 5))
        (do (>!! ba (msg 6 6))
            (let [b->a6 (<!!-test ba)]
              (is (=  b->a6 6))
              (is (nil? (get-active-interaction (get-monitor ab))))))))))


(deftest send-and-receive-parallel-after-choice-with-after-test
  (let [channels (add-infrastructure (parallel-after-choice-with-after true))
        ab (get-channel channels "a" "b")
        ba (get-channel channels "b" "a")]
    (>!! ab (msg 0 0))
    (let [a->b (<!!-test ab)]
      (is (=  a->b 0))
      (do (>!! ba (msg 2 2))
          (let [b->a2 (<!!-test ba)]
            (is (=  b->a2 2))
            (>!! ab (msg 3 3))
            (is (= (<!!-test ab) 3))))
      (do (>!! ba (msg 4 4))
          (let [b->a4 (<!!-test ba)]
            (is (=  b->a4 4))
            (>!! ab (msg 5 5))
            (is (= (<!!-test ab) 5))))
      (do (>!! ba (msg 6 6))
          (let [b->a6 (<!!-test ba)]
            (is (=  b->a6 6)))))))

(deftest send-and-receive-parallel-after-choice-with-after-choice-test
  (let [channels (add-infrastructure (parallel-after-choice-with-after-choice true))
        ab (get-channel channels "a" "b")
        ba (get-channel channels "b" "a")]
    (>!! ab (msg 0 0))
    (let [a->b (<!!-test ab)]
      (is (=  a->b 0))
      (do (>!! ba (msg 2 2))
          (let [b->a2 (<!!-test ba)]
            (is (=  b->a2 2))
            (>!! ab (msg 3 3))
            (is (= (<!!-test ab) 3))))
      (do (>!! ba (msg 4 4))
          (let [b->a4 (<!!-test ba)]
            (is (=  b->a4 4))
            (>!! ab (msg 5 5))
            (is (= (<!!-test ab) 5))))
      (do (>!! ba (msg 6 6))
          (let [b->a6 (<!!-test ba)]
            (is (=  b->a6 6)))))))

(deftest send-and-receive-parallel-after-rec-with-after-test
  (let [channels (add-infrastructure (parallel-after-rec-with-after true))
        ab (get-channel channels "a" "b")
        ba (get-channel channels "b" "a")]
    (>!! ab (msg 0 0))
    (let [a->b (<!!-test ab)]
      (is (=  a->b 0))
      (do (>!! ba (msg 2 2))
          (let [b->a2 (<!!-test ba)]
            (is (=  b->a2 2))
            (>!! ab (msg 3 3))
            (is (= (<!!-test ab) 3))))
      (do (>!! ba (msg 4 4))
          (let [b->a4 (<!!-test ba)]
            (is (=  b->a4 4))
            (>!! ab (msg 5 5))
            (is (= (<!!-test ab) 5))))
      (do (>!! ba (msg 6 6))
          (let [b->a6 (<!!-test ba)]
            (is (=  b->a6 6)))))))

(deftest send-and-receive-parallel-after-rec-with-after-rec-test
  (let [channels (add-infrastructure (parallel-after-rec-with-after-rec true))
        ab (get-channel channels "a" "b")
        ba (get-channel channels "b" "a")]
    (>!! ab (msg 0 0))
    (let [a->b (<!!-test ab)]
      (is (=  a->b 0))
      (do (>!! ba (msg 2 2))
          (let [b->a2 (<!!-test ba)]
            (is (=  b->a2 2))
            (>!! ab (msg 3 3))
            (is (= (<!!-test ab) 3))))
      (do (>!! ba (msg 4 4))
          (let [b->a4 (<!!-test ba)]
            (is (=  b->a4 4))
            (>!! ab (msg 5 5))
            (is (= (<!!-test ab) 5))))
      (do (>!! ba (msg 7 7))
          (let [b->a6 (<!!-test ba)]
            (is (=  b->a6 7)))))))

(deftest send-and-receive-parallel-after-rec-with-after-rec-with-recur-test
  (let [channels (add-infrastructure (parallel-after-rec-with-after-rec true))
        ab (get-channel channels "a" "b")
        ba (get-channel channels "b" "a")]
    (>!! ab (msg 0 0))
    (let [a->b (<!!-test ab)]
      (is (=  a->b 0))
      (do (>!! ba (msg 2 2))
          (let [b->a2 (<!!-test ba)]
            (is (=  b->a2 2))
            (>!! ab (msg 3 3))
            (is (= (<!!-test ab) 3))))
      (do (>!! ba (msg 4 4))
          (let [b->a4 (<!!-test ba)]
            (is (=  b->a4 4))
            (>!! ab (msg 5 5))
            (is (= (<!!-test ab) 5))))
      (do (>!! ba (msg 6 6))
          (let [b->a6 (<!!-test ba)]
            (is (=  b->a6 6))))
      (do (>!! ba (msg 7 7))
          (let [b->a7 (<!!-test ba)]
            (is (=  b->a7 7)))))))

(deftest send-and-receive-nested-parallel-test
  (let [channels (add-infrastructure (nested-parallel true))
        ab (get-channel channels "a" "b")
        ba (get-channel channels "b" "a")]
    (>!! ab (msg 1 1))
    (let [a->b (<!!-test ab)]
      (is (=  a->b 1))
      (do (>!! ba (msg 2 2))
          (let [b->a2 (<!!-test ba)]
            (is (=  b->a2 2))
            (>!! ab (msg 3 3))
            (is (= (<!!-test ab) 3)))
          (>!! ba (msg 4 4))
          (let [b->a4 (<!!-test ba)]
            (is (=  b->a4 4))
            (>!! ab (msg 5 5))
            (is (= (<!!-test ab) 5))))
      (do (>!! ba (msg "a" "a"))
          (let [b->aA (<!!-test ba)]
            (is (= b->aA "a"))
            (>!! ab (msg "b" "b"))
            (is (= (<!!-test ab) "b")))
          (>!! ba (msg "b" "b"))
          (let [b->aB (<!!-test ba)]
            (is (= b->aB "b"))
            (>!! ab (msg "a" "a"))
            (is (= (<!!-test ab) "a")))))))

(deftest send-and-receive-nested-parallel-Threaded-test
  (let [channels (add-infrastructure (nested-parallel true))
        ab (get-channel channels "a" "b")
        ba (get-channel channels "b" "a")
        fn-par-00 (fn []
                    (>!! ba (msg "a" "a"))
                    (<!!-test ba)
                    (>!! ab (msg "b" "b"))
                    (<!!-test ab))
        fn-par-01 (fn []
                    (>!! ba (msg "b" "b"))
                    (<!!-test ba)
                    (>!! ab (msg "a" "a"))
                    (<!!-test ab))
        fn-par-10 (fn []
                    (>!! ba (msg 2 2))
                    (<!!-test ba)
                    (>!! ab (msg 3 3))
                    (<!!-test ab))
        fn-par-11 (fn []
                    (>!! ba (msg 4 4))
                    (<!!-test ba)
                    (>!! ab (msg 5 5))
                    (<!!-test ab))
        ]
    (>!! ab (msg 1 1))
    (let [a->b (<!!-test ab)
          f00 (async/thread (fn-par-00))
          f01 (async/thread (fn-par-01))
          f10 (async/thread (fn-par-10))
          f11 (async/thread (fn-par-11))]
      (is (= a->b) 1)
      (is (= (async/<!! f00) "b"))
      (is (= (async/<!! f01) "a"))
      (is (= (async/<!! f10) 3))
      (is (= (async/<!! f11) 5))
      (is (nil? (get-active-interaction (get-monitor ab)))))))

(deftest send-and-receive-after-parallel-nested-parallel-test
  (let [channels (add-infrastructure (after-parallel-nested-parallel true))
        ab (get-channel channels "a" "b")
        ba (get-channel channels "b" "a")]
    (do
      (>!! ba (msg 0 0))
      (is (= (<!!-test ba) 0))
      (>!! ab (msg 1 1))
      (is (= (<!!-test ab) 1)))
    (do
      (>!! ba (msg "hi" "hi"))
      (is (= (<!!-test ba) "hi"))
      (>!! ab (msg "hi" "hi"))
      (is (= (<!!-test ab) "hi")))
    (do (>!! ba (msg 2 2))
        (let [b->a2 (<!!-test ba)]
          (is (=  b->a2 2))
          (>!! ab (msg 3 3))
          (is (= (<!!-test ab) 3)))
        (>!! ba (msg 4 4))
        (let [b->a4 (<!!-test ba)]
          (is (=  b->a4 4))
          (>!! ab (msg 5 5))
          (is (= (<!!-test ab) 5))))
    (do (>!! ba (msg "a" "a"))
        (let [b->aA (<!!-test ba)]
          (is (=  b->aA "a"))
          (>!! ab (msg "b" "b"))
          (is (= (<!!-test ab) "b")))
        (>!! ba (msg "b" "b"))
        (let [b->aB (<!!-test ba)]
          (is (=  b->aB "b"))
          (>!! ab (msg "a" "a"))
          (is (= (<!!-test ab) "a"))))))

(deftest send-and-receive-after-parallel-nested-parallel-Threaded-test
  (let [channels (add-infrastructure (after-parallel-nested-parallel true))
        ab (get-channel channels "a" "b")
        ba (get-channel channels "b" "a")
        fn-par0 (fn []
                  (>!! ba (msg 0 0))
                  (<!!-test ba)
                  (>!! ab (msg 1 1))
                  (<!!-test ab))
        fn-par1 (fn []
                  (>!! ba (msg "hi" "hi"))
                  (<!!-test ba)
                  (>!! ab (msg "hi" "hi"))
                  (<!!-test ab))
        fn-par-00 (fn []
                    (>!! ba (msg "a" "a"))
                    (<!!-test ba)
                    (>!! ab (msg "b" "b"))
                    (<!!-test ab))
        fn-par-01 (fn []
                    (>!! ba (msg "b" "n"))
                    (<!!-test ba)
                    (>!! ab (msg "a" "a"))
                    (<!!-test ab ))
        fn-par-10 (fn []
                    (>!! ba (msg 2 2))
                    (<!!-test ba)
                    (>!! ab (msg 3 3))
                    (<!!-test ab))
        fn-par-11 (fn []
                    (>!! ba (msg 4 4))
                    (<!!-test ba)
                    (>!! ab (msg 5 5))
                    (<!!-test ab))
        ]
    (let [f0 (async/thread (fn-par0))
          f1 (async/thread (fn-par1))]
      (is (= (async/<!! f0) 1))
      (is (= (async/<!! f1) "hi"))
      (let [f00 (async/thread (fn-par-00))
            f01 (async/thread (fn-par-01))
            f10 (async/thread (fn-par-10))
            f11 (async/thread (fn-par-11))]
        (is (=  (async/<!! f00) "b"))
        (is (=  (async/<!! f01) "a"))
        (is (=  (async/<!! f10) 3))
        (is (=  (async/<!! f11) 5))
        (is (nil? (get-active-interaction (get-monitor ab))))))))

(deftest send-and-receive-parallel-with-choice-test
  (let [channels (add-infrastructure (parallel-with-choice true))
        ab (get-channel channels "a" "b")
        ba (get-channel channels "b" "a")]
    (set-logging-exceptions)
    (>!! ab (msg 0 0))
    (let [a->b (<!!-test ab)]
      (is (=  a->b 0))
      (do (>!! ba (msg 4 4))
          (let [b->a4 (<!!-test ba)]
            (is (=  b->a4 4))
            (>!! ab (msg 5 5))
            (is (= (<!!-test ab) 5))))
      (do (>!! ba (msg 6 6))
          (let [b->a6 (<!!-test ba)]
            (is (=  b->a6 6))
            (is (nil? (get-active-interaction (get-monitor ab)))))))))

(deftest send-and-receive-parallel-with-choice-with-parallel-test
  (let [channels (add-infrastructure (parallel-with-choice-with-parallel true))
        ab (get-channel channels "a" "b")
        ba (get-channel channels "b" "a")]
    (set-logging-exceptions)
    (>!! ab (msg 0 0))
    (let [a->b (<!!-test ab)]
      (is (=  a->b 0))
      (do (>!! ba (msg 4 4))
          (let [b->a4 (<!!-test ba)]
            (is (= b->a4 4))
            (>!! ab (msg 5 5))
            (is (= (<!!-test ab) 5))))
      (do (>!! ba (msg "hi" "hi"))
          (let [b->ahi (<!!-test ba)]
            (is (= b->ahi "hi"))
            (>!! ab (msg "hi" "hi"))
            (is (= (<!!-test ab) "hi"))))
      (do (>!! ba (msg 6 6))
          (let [b->a6 (<!!-test ba)]
            (is (=  b->a6 6))
            (is (nil? (get-active-interaction (get-monitor ab)))))))))

(deftest send-and-receive-parallel-with-choice-with-parallel-multicasts-test
  (let [channels (add-infrastructure (parallel-with-choice-with-parallelMulticast true))
        ab (get-channel channels "a" "b")
        ac (get-channel channels "a" "c")
        ba (get-channel channels "b" "a")
        bc (get-channel channels "b" "c")]
    (set-logging-exceptions)
    (>!! [ab ac] (msg 0 0))
    (let [a->b (<!!-test ab)
          a->c (<!!-test ac)]
      (is (= a->b 0))
      (is (= a->c 0))
      (do (>!! [ba bc] (msg 4 4))
          (let [b->a4 (<!!-test ba)
                b->c4 (<!!-test bc)]
            (is (= b->a4 4))
            (is (= b->c4 4))
            (>!! [ab ac] (msg 5 5))
            (is (= (<!!-test ab) 5))
            (is (= (<!!-test ac) 5))))
      (do (>!! [ba bc] (msg "hi" "hi"))
          (let [b->ahi (<!!-test ba )
                b->chi (<!!-test bc )]
            (is (= b->ahi "hi"))
            (is (= b->chi "hi"))
            (>!! [ab ac] (msg "hi" "hi"))
            (is (= (<!!-test ab ) "hi"))
            (is (= (<!!-test ac ) "hi"))))
      (do (>!! [ba bc] (msg 6 6))
          (let [b->a6 (<!!-test ba)
                b->c6 (<!!-test bc)]
            (is (= b->a6 6))
            (is (= b->c6 6))
            (is (nil? (get-active-interaction (get-monitor ab)))))))))

(deftest send-and-receive-parallel-with-rec-test
  (let [channels (add-infrastructure (parallel-with-rec true))
        ab (get-channel channels "a" "b")
        ba (get-channel channels "b" "a")]
    (println (get-active-interaction (get-monitor ab)))
    (set-logging-exceptions)
    (loop [reps 0]
      (if (> reps 2)
        (do (>!! ab (msg 1 1))
            (is (= (<!!-test ab) 1)))
        (do (>!! ab (msg 0 0))
            (is (= (<!!-test ab) 0))
            (recur (+ reps 1)))))
    (do (>!! ba (msg 4 4))
        (let [b->a4 (<!!-test ba)]
          (is (= b->a4 4))
          (>!! ab (msg 5 5))
          (is (= (<!!-test ab) 5))))
    (do (>!! ba (msg 6 6))
        (let [b->a6 (<!!-test ba)]
          (is (= b->a6 6))
          (is (nil? (get-active-interaction (get-monitor ab))))))))

(deftest send-and-receive-rec-with-parallel-with-choice-test
  (let [channels (add-infrastructure (rec-with-parallel-with-choice true))
        ab (get-channel channels "a" "b")
        ba (get-channel channels "b" "a")]
    (set-logging-exceptions)
    (loop [reps 0]
      (if (> reps 2)
        (do
          (>!! ab (msg 1 1))
          (is (= (<!!-test ab) 1)))
        (do (>!! ab (msg 0 0))
            (is (= (<!!-test ab) 0))
            (do (>!! ba (msg 4 4))
                (let [b->a4 (<!!-test ba)]
                  (is (= b->a4 4))
                  (>!! ab(msg 5 5))
                  (is (= (<!!-test ab) 5))))
            (recur (+ reps 1)))))
    (do
      (>!! ba (msg 6 6))
      (let [b->a6 (<!!-test ba)]
        (is (= b->a6 6))
        (is (nil? (get-active-interaction (get-monitor ab))))))))

(deftest send-and-receive-rec-with-parallel-with-choice-multicast-test
  (let [channels (add-infrastructure (rec-with-parallel-with-choice-multicast true))
        ab (get-channel channels "a" "b")
        ac (get-channel channels "a" "c")
        ba (get-channel channels "b" "a")
        bc (get-channel channels "b" "c")]
    (set-logging-exceptions)
    (loop [reps 0]
      (if (> reps 2)
        (do
          (>!! [ab ac] (msg 1 1))
          (is (= (<!!-test ab) 1))
          (is (= (<!!-test ac) 1)))
        (do (>!! [ab ac] (msg 0 0))
            (is (= (<!!-test ab) 0))
            (is (= (<!!-test ac) 0))
            (do (>!! [ba bc] (msg 4 4))
                (let [b->a4 (<!!-test ba)
                      b->c4 (<!!-test bc)]
                  (is (= b->a4 4))
                  (is (= b->c4 4))
                  (>!! [ab ac](msg 5 5))
                  (is (= (<!!-test ab) 5))
                  (is (= (<!!-test ac) 5))))
            (recur (+ reps 1)))))
    (do
      (>!! [ba bc] (msg 6 6))
      (let [b->a6 (<!!-test ba)
            b->c6 (<!!-test bc)]
        (is (= b->a6 6))
        (is (= b->c6 6))
        (is (nil? (get-active-interaction (get-monitor ab))))))))

;(deftest send-and-receive-multiple-branches-choice-Threaded-test
;  (let [channels (add-infrastructure (multiple-branches-choice true))
;        ab (get-channel channels "a" "b")
;        ba (get-channel channels "b" "a")
;        fn-01 (fn []
;                (try+ (do
;                        (>!! ab (msg 0 0))
;                        (<!! ab 0)
;                        (>!! ba (msg 1 1))
;                        (<!! ba 1))
;                      (catch Object e
;                        (println (:throwable e) "unexpected error"))))
;        fn-23 (fn []
;                (try+ (do
;                        (>!! ab (msg 2 2))
;                        (<!! ab 2)
;                        (>!! ba (msg 3 3))
;                        (<!! ba 3))
;                      (catch Object e
;                        (println (:throwable e) "unexpected error"))))
;        fn-45 (fn []
;                (try+ (do
;                        (>!! ab (msg 4 4))
;                        (<!! ab 4)
;                        (>!! ba (msg 5 5))
;                        (<!! ba 5))
;                      (catch Object e
;                        (println (:throwable e) "unexpected error"))))
;        ]
;    (async/thread fn-01)
;    (async/thread fn-23)
;    (async/thread fn-45)
;    (is (nil? (get-active-interaction (get-monitor ab))))
;    ))

(deftest send-and-receive-parallel-after-interaction-multicast-test
  (let [channels (add-infrastructure (parallel-after-interaction-multicast true))
        ab (get-channel channels "a" "b")
        ba (get-channel channels "b" "a")
        ac (get-channel channels "a" "c")
        bc (get-channel channels "b" "c")]
    (>!! ab (msg 1 1))
    (let [a->b (<!!-test ab)]
      (is (= a->b) 1)
      (do (>!! [ba bc] (msg 2 2))
          (let [b->a2 (<!!-test ba)
                b->c2 (<!!-test bc)]
            (is (= b->a2 2))
            (is (= b->c2 2))
            (>!! [ab ac] (msg 3 3))
            (is (= (<!!-test ab) 3))
            (is (= (<!!-test ac) 3))))
      (do (>!! ba (msg 4 4))
          (let [b->a4 (<!!-test ba)]
            (is (= b->a4 4))
            (>!! ab (msg 5 5))
            (is (= (<!!-test ab) 5))))
      (do (>!! [ba bc] (msg 6 6))
          (is (= (<!!-test ba) 6))
          (is (= (<!!-test bc) 6))
          (is (nil? (get-active-interaction (get-monitor ab))))))))

(deftest send-and-receive-parallel-after-choice-with-after-choice-multicast-test
  (let [channels (add-infrastructure (parallel-after-choice-with-after-choice-multicast true))
        ab (get-channel channels "a" "b")
        ba (get-channel channels "b" "a")
        ac (get-channel channels "a" "c")
        bc (get-channel channels "b" "c")]
    (set-logging-exceptions)
    (>!! ab (msg 1 1))
    (let [a->b (<!!-test ab)]
      (is (= a->b 1))
      (do (>!! [ba bc] (msg 2 2))
          (let [b->a2 (<!!-test ba)
                b->c2 (<!!-test bc)]
            (is (= b->a2 2))
            (is (= b->c2 2))
            (>!! [ab ac] (msg 3 3))
            (is (= (<!!-test ab) 3))
            (is (= (<!!-test ac) 3))))
      (do (>!! ba (msg 4 4))
          (let [b->a4 (<!!-test ba)]
            (is (= b->a4 4))
            (>!! ab (msg 5 5))
            (is (= (<!!-test ab) 5))))
      (do (>!! [ba bc] (msg 6 6))
          (is (= (<!!-test ba) 6))
          (is (= (<!!-test bc) 6))
          (is (nil? (get-active-interaction (get-monitor ab))))))))

(deftest send-and-receive-parallel-after-rec-with-after-rec--multicast-test
  (let [channels (add-infrastructure (parallel-after-rec-with-after-rec-multicasts true))
        ab (get-channel channels "a" "b")
        ba (get-channel channels "b" "a")
        ac (get-channel channels "a" "c")
        bc (get-channel channels "b" "c")]
    (set-logging-exceptions)
    (>!! ab (msg 0 0 ))
    (let [a->b (<!!-test ab)]
      (is (= a->b 0))
      (do (>!! [ba bc] (msg 2 2))
          (let [b->a2 (<!!-test ba)
                b->c2 (<!!-test bc)]
            (is (= b->a2 2))
            (is (= b->c2 2))
            (>!! [ab ac] (msg 3 3))
            (is (= (<!!-test ab) 3))
            (is (= (<!!-test ac) 3))))
      (do (>!! ba (msg 4 4))
          (let [b->a4 (<!!-test ba)]
            (is (= b->a4 4))
            (>!! ab (msg 5 5))
            (is (= (<!!-test ab) 5))))
      (do (>!! [ba bc] (msg 6 6))
          (is (= (<!!-test ba) 6))
          (is (= (<!!-test bc) 6)))
      (do (>!! ba (msg 7 7))
          (is (= (<!!-test ba) 7))
          (is (nil? (get-active-interaction (get-monitor ab))))))))