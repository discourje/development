(ns discourje.async.operationTests
  (:require [clojure.test :refer :all]
            [discourje.async.protocolTestData :refer :all]
            [discourje.core.async.async :refer :all]
            [clojure.core.async :as async]))

(deftest send-test
  (let [channels (generate-infrastructure (testDualProtocol))
        c (get-channel "A" "B" channels)]
    (>!!! c (->message "1" "hello world"))
    (let [m (async/<!! (get-chan c))]
      (is (= "1" (get-label m)))
      (is (= "hello world" (get-content m))))))

(deftest receive-test
  (let [channels (generate-infrastructure (testDualProtocol))
        c (get-channel "A" "B" channels)]
    (async/>!! (get-chan c) (->message "1" "hello world"))
    (let [m (<!!! c "1")]
      (is (= "1" (get-label m)))
      (is (= "hello world" (get-content m))))))

(deftest send-receive-dual-test
  (let [channels (generate-infrastructure (testDualProtocol))
        ab (get-channel "A" "B" channels)
        ba (get-channel "B" "A" channels)
        m1 (->message "1" "Hello B")
        m2 (->message "2" "Hello A")]
    (do
      (>!!! ab m1)
      (let [a->b (<!!! ab "1")]
        (is (= "1" (get-label a->b)))
        (is (= "Hello B" (get-content a->b))))
      (>!!! ba m2)
      (let [b->a (<!!! ba "2")]
        (is (= "2" (get-label b->a)))
        (is (= "Hello A" (get-content b->a)))))))

(deftest send-receive-parallel-protocol-test
  (let [channels (generate-infrastructure (testParallelProtocol))
        ab (get-channel "A" "B" channels)
        ba (get-channel "B" "A" channels)
        ac (get-channel "A" "C" channels)
        ca (get-channel "C" "A" channels)
        cb (get-channel "C" "B" channels)]
    (do
      (>!!! ab (->message "1" "A->B"))
      (let [a->b (<!!! ab "1")]
        (is (= "1" (get-label a->b)))
        (is (= "A->B" (get-content a->b)))
        (>!!! ba (->message "2" "B->A"))
        (let [b->a (<!!! ba "2")]
          (is (= "2" (get-label b->a)))
          (is (= "B->A" (get-content b->a)))
          (>!!! ac (->message "3" "A->C"))
          (let [a->c (<!!! ac "3")]
            (is (= "3" (get-label a->c)))
            (is (= "A->C" (get-content a->c)))
            (>!!! [ca cb] (->message "4" "C->A-B"))
            (let [c->a (<!!! ca "4")
                  c->b (<!!! cb "4")]
              (is (= "4" (get-label c->a)))
              (is (= "C->A-B" (get-content c->a)))
              (is (= "4" (get-label c->b)))
              (is (= "C->A-B" (get-content c->b))))))))))

;(deftest send-receive-quad-protocol-test
;  (let [channels (generate-infrastructure (testQuadProtocol))
;        mainA (get-channel "main" "A" channels)
;        mainB (get-channel "main" "B" channels)
;        mainC (get-channel "main" "C" channels)
;        ab (get-channel "A" "B" channels)
;        ba (get-channel "B" "A" channels)
;        ac (get-channel "A" "C" channels)
;        ca (get-channel "C" "A" channels)
;        cb (get-channel "C" "B" channels)
;        ]))

(deftest send-receive-single-parallel-test
  (let [channels (generate-infrastructure (testSingleParallelProtocol))
        ab (get-channel "A" "B" channels)
        ac (get-channel "A" "C" channels)
        m1 (->message "1" "Hello B and C")]
    (do
      (>!!! [ab ac] m1)
      (let [a->b (<!!! ab "1")
            a->c (<!!! ac "1")]
        (is (= "1" (get-label a->b)))
        (is (= "Hello B and C" (get-content a->b)))
        (is (= (get-label a->c) (get-label a->b)))
        (is (= (get-content a->c) (get-content a->b)))))))

(deftest send-receive-single-Always0-choice-protocol
  (let [channels (generate-infrastructure (single-choice-protocol))
        ab (get-channel "A" "B" channels)
        ma (->message "1" "Hello B")]
    (do
      (>!!! ab ma)
      (let [a->b (<!!! ab "1")]
        (is (= "1" (get-label a->b)))
        (is (= "Hello B" (get-content a->b)))))))

(deftest send-receive-single-always1-choice-protocol
  (let [channels (generate-infrastructure (single-choice-protocol))
        ac (get-channel "A" "C" channels)
        mc (->message "hi" "Hi C")]
    (do
      (>!!! ac mc)
      (let [a->c (<!!! ac "hi")]
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
        (>!!! ab ma)
        (let [a->b (<!!! ab "1")]
          (is (= "1" (get-label a->b)))
          (is (= "Hello B" (get-content a->b)))))
      (do
        (>!!! ac mc)
        (let [a->c (<!!! ac "hi")]
          (is (= "hi" (get-label a->c)))
          (is (= "Hi C" (get-content a->c))))))))

(deftest send-receive-single-choice-in-middle-always0-choice-protocol
  (let [channels (generate-infrastructure (single-choice-in-middle-protocol))
        sf (get-channel "Start" "Finish" channels)
        fs (get-channel "Finish" "Start" channels)
        ab (get-channel "A" "B" channels)
        ba (get-channel "B" "A" channels)
        msf (->message "99" "Starting!")
        mab (->message "1" "1B")
        mba (->message "bla" "blaA")
        mfs (->message "88" "ending!")]
    (do
      (>!!! sf msf)
      (let [s->f (<!!! sf "99")]
        (is (= "99" (get-label s->f)))
        (is (= "Starting!" (get-content s->f)))
        (>!!! ab mab)
        (let [a->b (<!!! ab "1")]
          (is (= "1" (get-label a->b)))
          (is (= "1B" (get-content a->b)))
          (>!!! ba mba)
          (let [b->a (<!!! ba "bla")]
            (is (= "bla" (get-label b->a)))
            (is (= "blaA" (get-content b->a)))
            (>!!! fs mfs)
            (let [f->s (<!!! fs "88")]
              (is (= "88" (get-label f->s)))
              (is (= "ending!" (get-content f->s))))))))))

(deftest send-receive-single-choice-multiple-interactions-protocol-test
  (let [channels (generate-infrastructure (single-choice-multiple-interactions-protocol))
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
    (do (>!!! ab mab1)
        (let [a->b (<!!! ab "1")]
          (is (= "1" (get-label a->b)))
          (is (= "1ab" (get-content a->b)))
          (>!!! ba mab1)
          (let [b->a (<!!! ba "1")]
            (is (= "1" (get-label b->a)))
            (is (= "1ab" (get-content b->a)))
            (>!!! ac mab-c2)
            (let [a->c (<!!! ac "2")]
              (is (= "2" (get-label a->c)))
              (is (= "B or C" (get-content a->c)))
              (>!!! ca mab-c2)
              (let [c->a (<!!! ca "2")]
                (is (= "2" (get-label c->a)))
                (is (= "B or C" (get-content c->a)))
                (>!!! ac mab-c3)
                (let [a->c3 (<!!! ac "3")]
                  (is (= "3" (get-label a->c3)))
                  (is (= "B or C" (get-content a->c3)))
                  (>!!! ca mab-c3)
                  (let [c->a3 (<!!! ca "3")]
                    (is (= "3" (get-label c->a3)))
                    (is (= "B or C" (get-content c->a3)))
                    (>!!! ad mad)
                    (let [a->d (<!!! ad "4")]
                      (is (= "4" (get-label a->d)))
                      (is (= "4d" (get-content a->d)))
                      (>!!! da mad)
                      (let [d->a (<!!! da "4")]
                        (is (= "4" (get-label d->a)))
                        (is (= "4d" (get-content d->a)))
                        (>!!! [ab ac ad] m5)
                        (let [a->b5 (<!!! ab "5")
                              a->c5 (<!!! ac "5")
                              a->d5 (<!!! ad "5")]
                          (is (= "5" (get-label a->b5)))
                          (is (= "bye all" (get-content a->b5)))
                          (is (= "5" (get-label a->c5)))
                          (is (= "bye all" (get-content a->c5)))
                          (is (= "5" (get-label a->d5)))
                          (is (= "bye all" (get-content a->d5)))))))))))))))

(deftest send-receive-single-recur-protocol
  (let [channels (generate-infrastructure (single-recur-protocol))
        ab (get-channel "A" "B" channels)
        ba (get-channel "B" "A" channels)
        ac (get-channel "A" "C" channels)
        ca (get-channel "C" "A" channels)
        flag (atom false)]
    (do (>!!! ab (->message "1" "AB"))
        (let [a->b (<!!! ab "1")]
          (is (= "1" (get-label a->b)))
          (is (= "AB" (get-content a->b)))
          (while (false? @flag)
            (>!!! ba (->message "1" "AB"))
            (let [b->a (<!!! ba "1")]
              (is (= "1" (get-label b->a)))
              (is (= "AB" (get-content b->a)))
            (if (== 1 (+ 1 (rand-int 2)))
              (do
                  (>!!! ac (->message "2" "AC"))
                  (let [a->c (<!!! ac "2")]
                    (is (= "2" (get-label a->c)))
                    (is (= "AC" (get-content a->c)))
                    (>!!! ca (->message "2" "AC"))
                    (let [c->a (<!!! ca "2")]
                      (is (= "2" (get-label c->a)))
                      (is (= "AC" (get-content c->a))))))
                (do
                  (>!!! ab (->message "3" "AB3"))
                  (let [a->b3 (<!!! ab "3")]
                    (is (= "3" (get-label a->b3)))
                    (is (= "AB3" (get-content a->b3)))
                    (reset! flag true))))))
            (>!!! [ab ac] (->message "end" "ending"))
            (let [a->b-end (<!!! ab "end")
                  a->c-end (<!!! ac "end")]
              (is (= "end" (get-label a->b-end)))
              (is (= "ending" (get-content a->b-end)))
              (is (= "end" (get-label a->c-end)))
              (is (= "ending" (get-content a->c-end))))))))