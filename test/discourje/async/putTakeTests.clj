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


(deftest send-and-receive-parallel-after-rec-with-after-rec--multicast-test
  (let [channels (add-infrastructure (parallel-after-rec-with-after-rec-multicasts true))
        ab (get-channel channels "a" "b")
        ba (get-channel channels "b" "a")
        ac (get-channel channels "a" "c")
        bc (get-channel channels "b" "c")]
    (put! ab (msg 0 0)
          (fn [a]
            (take! ab (fn [b]
                        (is (= (get-content b) 0))
                        (put! [ba bc] (msg 2 2)
                              (fn [c]
                                (take! ba
                                       (fn [d] (do (is (= (get-content d) 2))
                                                   (take! bc
                                                          (fn [e] (do (is (= (get-content e) 2))
                                                                      (put! [ab ac] (msg 3 3)
                                                                            (fn [f]
                                                                              (take! ab
                                                                                     (fn [g] (do (is (= (get-content g) 3))
                                                                                                 (take! ac
                                                                                                        (fn [h] (do (is (= (get-content h) 3))

                                                                                                                    ))))))

                                                                              ))

                                                                      ))))))

                                ))
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
                            (is (nil? (get-active-interaction (get-monitor ab)))))

                        ))
            ))

    (>!! ab (msg 0 0))
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