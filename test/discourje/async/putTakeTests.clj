(ns discourje.async.putTakeTests
  (:require [clojure.test :refer :all]
            [discourje.core.async :refer :all]
            [discourje.async.protocolTestData :refer :all]
            [discourje.async.operationTests :refer :all]
            [clojure.core.async :as async]
            [discourje.async.parameterizedRecTests :refer :all]))

(deftest put-test
  (let [channels (generate-infrastructure (testDualProtocol true))
        c (get-channel channels "A" "B")]
    (put! c (msg "1" "hello world")
          (fn [x]
            (let [m (async/<!! (get-chan c))]
              (is (= "1" (get-label m)))
              (is (= "hello world" (get-content m))))))))

(deftest put-take-test
  (let [channels (generate-infrastructure (testDualProtocol true))
        c (get-channel channels "A" "B")]
    (put! c (msg "1" "hello world")
          (fn [x]
            (println x)
            (take! c (fn [v] (is (= "1" (get-label v)))
                       (is (= "hello world" (get-content v)))))))))

(deftest put-Take-dual-test
  (let [channels (generate-infrastructure (testDualProtocol true))
        ab (get-channel channels "A" "B")
        ba (get-channel channels "B" "A")
        m1 (->message "1" "Hello B")
        m2 (->message "2" "Hello A")]
    (put! ab m1 (fn [x]
                  (take! ab
                         (fn [x] (do
                                   (is (= "Hello B" (get-content x)))
                                   (put! ba m2
                                         (fn [x] (take! ba
                                                        (fn [x]
                                                          (is (= "Hello A" (get-content x))))))))))))))

(deftest put-take-close-interaction-with-rec-and-closer-test
  (let [channels (add-infrastructure (interaction-with-rec-and-closer true))
        ab (get-channel channels "a" "b")]
    (put! ab (msg 0 0) (fn [x]
                         (take! ab (fn [v]
                                     (is (= (get-content v) 0))
                                     (close-channel! ab)
                                     (is true (channel-closed? ab))
                                     (is (nil? (get-active-interaction (get-monitor ab))))
                                     ))))))

(deftest put-take-single-multicast-test
  (let [channels (generate-infrastructure (testSingleMulticastProtocol))
        ab (get-channel channels "A" "B")
        ac (get-channel channels "A" "C")
        m1 (->message "1" "Hello B and C")]
    (put! [ab ac] m1 (fn [x]
                       (take! ab (fn [v] (is (= "Hello B and C" (get-content v)))))
                       (take! ac (fn [v] (is (= "Hello B and C" (get-content v)))))))))

(deftest put-and-take-rec-with-parallel-with-choice-multicast-and-close-test
  (let [channels (add-infrastructure (rec-with-parallel-with-choice-multicast-and-close true))
        ab (get-channel channels "a" "b")
        ac (get-channel channels "a" "c")
        ba (get-channel channels "b" "a")
        bc (get-channel channels "b" "c")]
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
                  (>!! [ab ac] (msg 5 5))
                  (is (= (<!!-test ab) 5))
                  (is (= (<!!-test ac) 5))))
            (recur (+ reps 1)))))
    (do
      (close-channel! ab)
      (is true (channel-closed? ab))
      (is true (channel-closed? (get-channel channels "a" "b")))
      (close-channel! "a" "c" channels)
      (is true (channel-closed? ac))
      (is true (channel-closed? (get-channel channels "a" "c")))
      (>!! [ba bc] (msg 6 6))
      (let [b->a6 (<!!-test ba)
            b->c6 (<!!-test bc)]
        (is (= b->a6 6))
        (is (= b->c6 6))
        (close-channel! ba)
        (close-channel! bc)
        (is true (channel-closed? ba))
        (is true (channel-closed? (get-channel channels "b" "a")))
        (is true (channel-closed? bc))
        (is true (channel-closed? (get-channel channels "b" "c")))
        (is (nil? (get-active-interaction (get-monitor ab))))))))