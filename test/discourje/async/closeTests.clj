(ns discourje.async.closeTests
  (:require [clojure.test :refer :all]
            [discourje.async.protocolTestData :refer :all]
            [discourje.core.async :refer :all]
            [clojure.core.async :as async]
            [discourje.core.logging :refer :all]))

(defn only-closer-protocol [include-ids]
  (if include-ids (create-protocol
                    [(close "alice" "bob")
                     (close "alice" "carol")])
                  (create-protocol
                    [(->closer nil "alice" "bob" nil)
                     (->closer nil "alice" "carol" nil)])))

(def only-closer-protocol-control
  (->closer nil "alice" "bob"
            (->closer nil "alice" "carol" nil)))

(deftest only-closer-protocol-test
  (let [mon (generate-monitor (only-closer-protocol false))]
    (is (= (get-active-interaction mon) only-closer-protocol-control))))

(defn <!!-test
  "Utility method to fix all test cases"
  [channel]
  (let [value (discourje.core.async/<!! channel)]
    (get-content value)))

(deftest close-interaction-with-closer-test
  (let [channels (add-infrastructure (interaction-with-closer true))
        ab (get-channel channels "a" "b")]
    (set-logging-exceptions)
    (>!! ab (msg 0 0))
    (let [a->b (<!!-test ab)]
      (is (= a->b 0))
      (close-channel! ab)
      (is true (channel-closed? ab)))
    (is (nil? (get-active-interaction (get-monitor ab))))))

(deftest channel-cannot-be-closed-before-receive-test       ;; active monitor is still the interation!
  (let [channels (add-infrastructure (interaction-with-closer true))
        ab (get-channel channels "a" "b")]
    (set-logging-exceptions)
    (>!! ab (msg 0 0))
    (is (thrown? Exception (close-channel! ab)))
    (let [a->b (<!!-test ab)]
      (is (= a->b 0))
      (close-channel! ab)
      (is true (channel-closed? ab)))
    (is (nil? (get-active-interaction (get-monitor ab))))))

(deftest close-interaction-with-choice-and-closer-test
  (let [channels (add-infrastructure (interaction-with-choice-and-closer true))
        ab (get-channel channels "a" "b")]
    (set-logging-exceptions)
    (>!! ab (msg 0 0))
    (let [a->b (<!!-test ab)]
      (is (= a->b 0))
      (close-channel! ab)
      (is true (channel-closed? ab)))
    (is (nil? (get-active-interaction (get-monitor ab))))))

(deftest close-interaction-with-rec-and-closer-test
  (let [channels (add-infrastructure (interaction-with-rec-and-closer true))
        ab (get-channel channels "a" "b")]
    (set-logging-exceptions)
    (>!! ab (msg 0 0))
    (let [a->b (<!!-test ab)]
      (is (= a->b 0))
      (close-channel! ab)
      (is true (channel-closed? ab)))
    (is (nil? (get-active-interaction (get-monitor ab))))))

(deftest close-interaction-with-rec-and-choice-and-closer-test
  (let [channels (add-infrastructure (interaction-with-rec-and-choice-and-closer true))
        ab (get-channel channels "a" "b")]
    (set-logging-exceptions)
    (>!! ab (msg 0 0))
    (let [a->b (<!!-test ab)]
      (is (= a->b 0))
      (close-channel! ab)
      (is true (channel-closed? ab)))
    (is (nil? (get-active-interaction (get-monitor ab))))))

(deftest close-interaction-with-parallel-and-closer-test
  (let [channels (add-infrastructure (interaction-with-parallel-and-closer true))
        ab (get-channel channels "a" "b")]
    (set-logging-exceptions)
    (>!! ab (msg 0 0))
    (let [a->b (<!!-test ab)]
      (is (= a->b 0))
      (let [a->b1 (>!! ab (msg 1 1))]
        (is (= (<!!-test ab)))
        (close-channel! ab)
        (is true (channel-closed? ab))))
    (is (nil? (get-active-interaction (get-monitor ab))))))

(deftest close-interaction-with-parallel-and-closer-with-interactions-in-parallel-test
  (let [channels (add-infrastructure (interaction-with-parallel-and-closer-with-interactions-in-parallel true))
        ab (get-channel channels "a" "b")
        ba (get-channel channels "b" "a")]
    (set-logging-exceptions)
    (>!! ab (msg 0 0))
    (let [a->b (<!!-test ab)]
      (is (= a->b 0))
      (>!! ab (msg 1 1))
      (let [a->b1 (<!!-test ab)]
        (is (= a->b1 1))
        (close-channel! ab)
        (is true (channel-closed? ab))
        (>!! ba (msg 2 2))
        (is (= (<!!-test ba) 2))
        (close-channel! ba)
        (is true (channel-closed? ba))))
    (is (nil? (get-active-interaction (get-monitor ab))))))

(deftest close-interaction-with-nested-choice-and-closer-test
  (let [channels (add-infrastructure (interaction-with-nested-choice-and-closer true))
        ab (get-channel channels "a" "b")]
    (set-logging-exceptions)
    (>!! ab (msg 0 0))
    (let [a->b (<!!-test ab)]
      (is (= a->b 0))
      (close-channel! ab)
      (is true (channel-closed? ab)))
    (is (nil? (get-active-interaction (get-monitor ab))))))

(deftest send-and-receive-rec-with-parallel-with-choice-multicast-and-close-test
  (let [channels (add-infrastructure (rec-with-parallel-with-choice-multicast-and-close true))
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

(deftest send-and-receive-after-parallel-nested-parallel-test
  (let [channels (add-infrastructure (after-parallel-nested-parallel-with-closer true))
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
    (do (>!! ba (msg "a" "a"))
        (let [b->aA (<!!-test ba)]
          (is (= b->aA "a"))
          (>!! ab (msg "b" "b"))
          (is (= (<!!-test ab) "b")))
        (>!! ba (msg "b" "b"))
        (let [b->aB (<!!-test ba)]
          (is (= b->aB "b"))
          (>!! ab (msg "a" "a"))
          (is (= (<!!-test ab) "a"))))
    (do (>!! ba (msg 2 2))
        (let [b->a2 (<!!-test ba)]
          (is (= b->a2 2))
          (>!! ab (msg 3 3))
          (is (= (<!!-test ab) 3)))
        (close-channel! ab)
        (close-channel! "b" "a" channels)
        (is true (channel-closed? ab))
        (is true (channel-closed? (get-channel channels "a" "b")))
        (is true (channel-closed? ba))
        (is true (channel-closed? (get-channel channels "b" "a"))))))

;(deftest send-and-receive-after-parallel-nested-parallel-Threaded-test
;  (let [channels (add-infrastructure (after-parallel-nested-parallel-with-closer true))
;        ab (get-channel channels "a" "b")
;        ba (get-channel channels "b" "a")
;        fn-par0 (fn []
;                  (>!! ba (msg 0 0))
;                  (<!! ba 0)
;                  (>!! ab (msg 1 1))
;                  (<!! ab 1))
;        fn-par1 (fn []
;                  (>!! ba (msg "hi" "hi"))
;                  (<!! ba "hi")
;                  (>!! ab (msg "hi" "hi"))
;                  (<!! ab "hi"))
;        fn-par-00 (fn []
;                    (>!! ba (msg "a" "a"))
;                    (<!! ba "a")
;                    (>!! ab (msg "b" "b"))
;                    (<!! ab "b"))
;        fn-par-01 (fn []
;                    (>!! ba (msg "b" "a"))
;                    (<!! ba "b")
;                    (>!! ab (msg "a" "a"))
;                    (<!! ab "a"))
;        fn-par-10 (fn []
;                    (>!! ba (msg 2 2))
;                    (<!! ba 2)
;                    (>!! ab (msg 3 3))
;                    (<!! ab 3))
;        fn-par-11 (fn []
;                    (close-channel! ab)
;                    (close-channel! "b" "a" channels))
;        ]
;    (let [f0 (async/thread (fn-par0))
;          f1 (async/thread (fn-par1))]
;      (is (= (get-label (async/<!! f0)) 1))
;      (is (= (get-label (async/<!! f1)) "hi"))
;      (let [f00 (async/thread (fn-par-00))
;            f01 (async/thread (fn-par-01))
;            f10 (async/thread (fn-par-10))
;            f11 (async/thread (fn-par-11))]
;        (is (= (get-label (async/<!! f00)) "b"))
;        (is (= (get-label (async/<!! f01)) "a"))
;        (is (= (get-label (async/<!! f10)) 3))
;        (let [x (async/<!! f11)]
;          (is true (channel-closed? ab))
;          (is true (channel-closed? (get-channel channels "a" "b")))
;          (is true (channel-closed? ba))
;          (is true (channel-closed? (get-channel channels "b" "a"))))
;        (is (nil? (get-active-interaction (get-monitor ab))))))))