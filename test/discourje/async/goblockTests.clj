(ns discourje.async.goblockTests
  (:require [clojure.test :refer :all]
            [discourje.core.async :refer :all]
            [discourje.async.protocolTestData :refer :all]
            [discourje.async.operationTests :refer :all]
            [discourje.async.parameterizedRecTests :refer :all]))

(defmacro <!-test
  "Utility method to fix all test cases"
  [channel]
  `(let [~'value (discourje.core.async/<! ~channel)]
     (get-content ~'value)))

(deftest go-send-receive-dual-test
  (let [channels (generate-infrastructure (testDualProtocol true))
        ab (get-channel channels "A" "B")
        ba (get-channel channels "B" "A")
        m1 (->message "1" "Hello B")
        m2 (->message "2" "Hello A")]
    (is (= "Hello A"
           (clojure.core.async/<!! (go
                                     (>! ab m1)
                                     (<!-test ab)
                                     (>! ba m2)
                                     (<!-test ba)))))))

(deftest go-send-receive-single-multicast-test
  (let [channels (generate-infrastructure (testSingleMulticastProtocol))
        ab (get-channel channels "A" "B")
        ac (get-channel channels "A" "C")
        m1 (->message "1" "Hello B and C")]
    (is (= "Hello B and C"
           (clojure.core.async/<!! (go
                                     (>! [ab ac] m1)
                                     (let [a->b (<!-test ab)
                                           a->c (<!-test ac)]
                                       a->c)
                                     ))))
    (is (nil? (get-active-interaction (get-monitor ab))))))

(deftest go-send-receive-multicast-protocol-test
  (let [channels (generate-infrastructure (testMulticastProtocol true))
        ab (get-channel channels "A" "B")
        ba (get-channel channels "B" "A")
        ac (get-channel channels "A" "C")
        ca (get-channel channels "C" "A")
        cb (get-channel channels "C" "B")
        m1 (->message "1" "A->B")
        m2 (->message "2" "B->A")
        m3 (->message "3" "A->C")
        m4 (->message "4" "C->A-B")]
    (is (= "C->A-B"
           (clojure.core.async/<!! (go
                                     (>! ab m1)
                                     (<!-test ab)
                                     (>! ba m2)
                                     (<!-test ba)
                                     (>! ac m3)
                                     (<!-test ac)
                                     (>! [ca cb] m4)
                                     (<!-test ca)
                                     (<!-test cb)))))
    (is (nil? (get-active-interaction (get-monitor ab))))))

(deftest go-send-and-receive-parallel-after-rec-with-after-rec--multicast-test
  (let [channels (add-infrastructure (parallel-after-rec-with-after-rec-multicasts true))
        ab (get-channel channels "a" "b")
        ba (get-channel channels "b" "a")
        ac (get-channel channels "a" "c")
        bc (get-channel channels "b" "c")]
    ; go blocks must be separated into two go blocks or else the compiler throws this exception:
    ; Syntax error (IndexOutOfBoundsException) compiling fn* at (test/discourje/async/goblockTests.clj:78:72).
    ; Method code too large!
    (is (= 4
           (clojure.core.async/<!! (go
                                     (>! ab (msg 0 0))
                                     (<!-test ab)
                                     (>! [ba bc] (msg 2 2))
                                     (<!-test ba)
                                     (<!-test bc)
                                     (>! [ab ac] (msg 3 3))
                                     (<!-test ab)
                                     (<!-test ac)
                                     (>! ba (msg 4 4))
                                     (<!-test ba)
                                     ))))
    (is (= 7
           (clojure.core.async/<!! (go
                                     (>! ab (msg 5 5))
                                     (<!-test ab)
                                     (>! [ba bc] (msg 6 6))
                                     (<!-test ba)
                                     (<!-test bc)
                                     (>! ba (msg 7 7))
                                     (<!-test ba)))))
    (is (nil? (get-active-interaction (get-monitor ab))))))

(deftest go-close-interaction-with-closer-test
  (let [channels (add-infrastructure (interaction-with-closer true))
        ab (get-channel channels "a" "b")]
    (clojure.core.async/<!! (go
                              (>! ab (msg 0 0))
                              (<!-test ab)
                              (close-channel! ab)))
    (is true (channel-closed? ab))
    (is (nil? (get-active-interaction (get-monitor ab))))))

(deftest send-and-receive-rec-with-parallel-with-choice-multicast-and-close-test
  (let [channels (add-infrastructure (rec-with-parallel-with-choice-multicast-and-close true))
        ab (get-channel channels "a" "b")
        ac (get-channel channels "a" "c")
        ba (get-channel channels "b" "a")
        bc (get-channel channels "b" "c")]
    (loop [reps 0]
      (if (> reps 2)
        (is (= 1
               (clojure.core.async/<!! (go
                                         (do (>! [ab ac] (msg 1 1))
                                             (<!-test ab)
                                             (<!-test ac))))))
        (let [v (clojure.core.async/<!! (go
                                          (do (>! [ab ac] (msg 0 0))
                                              (<!-test ab)
                                              (<!-test ac)
                                              (do (>! [ba bc] (msg 4 4))
                                                  (<!-test ba)
                                                  (<!-test bc)
                                                  (>! [ab ac] (msg 5 5))
                                                  (<!-test ab)
                                                  (<!-test ac)))))]
          (is (== v 5))
          (recur (+ reps 1)))))
    (clojure.core.async/<!! (go
                              (close-channel! ab)
                              (close-channel! "a" "c" channels)
                              (>! [ba bc] (msg 6 6))
                              (<!-test ba)
                              (<!-test bc)
                              (close-channel! ba)
                              (close-channel! bc)))
    (is true (channel-closed? ab))
    (is true (channel-closed? (get-channel channels "a" "b")))
    (is true (channel-closed? ac))
    (is true (channel-closed? (get-channel channels "a" "c")))
    (is true (channel-closed? ba))
    (is true (channel-closed? (get-channel channels "b" "a")))
    (is true (channel-closed? bc))
    (is true (channel-closed? (get-channel channels "b" "c")))
    (is (nil? (get-active-interaction (get-monitor ab))))))

(deftest goloop-send-receive-one-recur-with-choice-protocol
  (let [channels (generate-infrastructure (one-recur-with-choice-protocol true))
        ab (get-channel channels "A" "B")
        ac (get-channel channels "A" "C")]
    (is (= "AB3"
           (clojure.core.async/<!! (go-loop [reps 0]
                                            (if (< reps 2)
                                              (do
                                                (>! ac (->message "2" "AC"))
                                                (<!-test ac)
                                                (recur (inc reps)))
                                              (do
                                                (>! ab (->message "3" "AB3"))
                                                (<!-test ab)))))))))


(deftest go-send-receive-single-recur-protocol-params
  (let [channels (generate-infrastructure (single-recur-protocol-params true))
        ab (get-channel channels "A" "B")
        ba (get-channel channels "B" "A")
        ac (get-channel channels "A" "C")
        ca (get-channel channels "C" "A")
        flag (atom false)]
    (is (= "ending"
           (clojure.core.async/<!! (go (>! ab (->message "1" "AB"))
                                       (<!-test ab)
                                       (while (false? @flag)
                                         (>! ba (->message "1" "AB"))
                                         (<!-test ba)
                                         (if (== 1 (+ 1 (rand-int 2)))
                                           (do
                                             (>! ac (->message "2" "AC"))
                                             (<!-test ac)
                                             (>! ca (->message "2" "AC"))
                                             (<!-test ca))
                                           (do
                                             (>! ab (->message "3" "AB3"))
                                             (<!-test ab)
                                             (reset! flag true))))
                                       (>! [ab ac] (->message "end" "ending"))
                                       (<!-test ab)
                                       (<!-test ac)))))))