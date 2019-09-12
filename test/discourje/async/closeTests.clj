(ns discourje.async.closeTests
  (:require [clojure.test :refer :all]
            [discourje.async.protocolTestData :refer :all]
            [discourje.core.async :refer :all]
            [clojure.core.async :as async]
            [discourje.core.logging :refer :all])
  (:use [slingshot.slingshot :only [throw+ try+]]))

(deftest close-interaction-with-closer-test
  (let [channels (add-infrastructure (interaction-with-closer true))
        ab (get-channel channels "a" "b")]
    (set-logging-exceptions)
    (>!! ab (msg 0 0))
    (let [a->b (<!! ab 0)]
      (is (= (get-label a->b) 0))
      (close-channel! ab)
      (is true (channel-closed? ab)))
    (is (nil? (get-active-interaction (get-monitor ab))))))

(deftest close-interaction-with-choice-and-closer-test
  (let [channels (add-infrastructure (interaction-with-choice-and-closer true))
        ab (get-channel channels "a" "b")]
    (set-logging-exceptions)
    (>!! ab (msg 0 0))
    (let [a->b (<!! ab 0)]
      (is (= (get-label a->b) 0))
      (close-channel! ab)
      (is true (channel-closed? ab)))
    (is (nil? (get-active-interaction (get-monitor ab))))))

(deftest close-interaction-with-rec-and-closer-test
  (let [channels (add-infrastructure (interaction-with-rec-and-closer true))
        ab (get-channel channels "a" "b")]
    (set-logging-exceptions)
    (>!! ab (msg 0 0))
    (let [a->b (<!! ab 0)]
      (is (= (get-label a->b) 0))
      (close-channel! ab)
      (is true (channel-closed? ab)))
    (is (nil? (get-active-interaction (get-monitor ab))))))

(deftest close-interaction-with-rec-and-choice-and-closer-test
  (let [channels (add-infrastructure (interaction-with-rec-and-choice-and-closer true))
        ab (get-channel channels "a" "b")]
    (set-logging-exceptions)
    (>!! ab (msg 0 0))
    (let [a->b (<!! ab 0)]
      (is (= (get-label a->b) 0))
      (close-channel! ab)
      (is true (channel-closed? ab)))
    (is (nil? (get-active-interaction (get-monitor ab))))))

(deftest close-interaction-with-parallel-and-closer-test
  (let [channels (add-infrastructure (interaction-with-parallel-and-closer true))
        ab (get-channel channels "a" "b")]
    (set-logging-exceptions)
    (>!! ab (msg 0 0))
    (let [a->b (<!! ab 0)]
      (is (= (get-label a->b) 0))
      (let [a->b1 (>!! ab (msg 1 1))]
        (is (= (get-label (<!! ab 1))))
        (close-channel! ab)
        (is true (channel-closed? ab))))
    (is (nil? (get-active-interaction (get-monitor ab))))))

(deftest close-interaction-with-parallel-and-closer-test
  (let [channels (add-infrastructure (interaction-with-parallel-and-closer-with-interactions-in-parallel true))
        ab (get-channel channels "a" "b")
        ba (get-channel channels "b" "a")]
    (set-logging-exceptions)
    (>!! ab (msg 0 0))
    (let [a->b (<!! ab 0)]
      (is (= (get-label a->b) 0))
      (>!! ab (msg 1 1))
      (let [a->b1 (get-label (<!! ab 1))]
        (is (= a->b1))
        (close-channel! ab)
        (is true (channel-closed? ab))
        (>!! ba (msg 2 2))
        (is (= (get-label (<!! ba 2))))
        (close-channel! ba)
        (is true (channel-closed? ba))))
    (is (nil? (get-active-interaction (get-monitor ab))))))

(deftest close-interaction-with-nested-choice-and-closer-test
  (let [channels (add-infrastructure (interaction-with-nested-choice-and-closer true))
        ab (get-channel channels "a" "b")]
    (set-logging-exceptions)
    (>!! ab (msg 0 0))
    (let [a->b (<!! ab 0)]
      (is (= (get-label a->b) 0))
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
          (is (= (get-label (<!! ab 1)) 1))
          (is (= (get-label (<!! ac 1)) 1)))
        (do (>!! [ab ac] (msg 0 0))
            (is (= (get-label (<!! ab 0)) 0))
            (is (= (get-label (<!! ac 0)) 0))
            (do (>!! [ba bc] (msg 4 4))
                (let [b->a4 (<!! ba 4)
                      b->c4 (<!! bc 4)]
                  (is (= (get-label b->a4) 4))
                  (is (= (get-label b->c4) 4))
                  (>!! [ab ac] (msg 5 5))
                  (is (= (get-label (<!! ab 5)) 5))
                  (is (= (get-label (<!! ac 5)) 5))))
            (recur (+ reps 1)))))
    (do
      (close-channel! ab)
      (is true (channel-closed? ab))
      (is true (channel-closed? (get-channel channels "a" "b")))
      (close-channel! "a" "c" channels)
      (is true (channel-closed? ac))
      (is true (channel-closed? (get-channel channels "a" "c")))
      (>!! [ba bc] (msg 6 6))
      (let [b->a6 (<!! ba 6)
            b->c6 (<!! bc 6)]
        (is (= (get-label b->a6) 6))
        (is (= (get-label b->c6) 6))
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
      (is (= (get-label (<!! ba 0)) 0))
      (>!! ab (msg 1 1))
      (is (= (get-label (<!! ab 1)) 1)))
    (do
      (>!! ba (msg "hi" "hi"))
      (is (= (get-label (<!! ba "hi")) "hi"))
      (>!! ab (msg "hi" "hi"))
      (is (= (get-label (<!! ab "hi")) "hi")))
    (do (>!! ba (msg "a" "a"))
        (let [b->aA (<!! ba "a")]
          (is (= (get-label b->aA) "a"))
          (>!! ab (msg "b" "b"))
          (is (= (get-label (<!! ab "b")) "b")))
        (>!! ba (msg "b" "a"))
        (let [b->aB (<!! ba "b")]
          (is (= (get-label b->aB) "b"))
          (>!! ab (msg "a" "a"))
          (is (= (get-label (<!! ab "a")) "a"))))
    (do (>!! ba (msg 2 2))
        (let [b->a2 (<!! ba 2)]
          (is (= (get-label b->a2) 2))
          (>!! ab (msg 3 3))
          (is (= (get-label (<!! ab 3)) 3)))
        (close-channel! ab)
        (close-channel! "b" "a" channels)
        (is true (channel-closed? ab))
        (is true (channel-closed? (get-channel channels "a" "b")))
        (is true (channel-closed? ba))
        (is true (channel-closed? (get-channel channels "b" "a"))))))

(deftest send-and-receive-after-parallel-nested-parallel-Threaded-test
  (let [channels (add-infrastructure (after-parallel-nested-parallel-with-closer true))
        ab (get-channel channels "a" "b")
        ba (get-channel channels "b" "a")
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
                    (close-channel! ab)
                    (close-channel! "b" "a" channels))
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
        (let [x (async/<!! f11)]
          (is true (channel-closed? ab))
          (is true (channel-closed? (get-channel channels "a" "b")))
          (is true (channel-closed? ba))
          (is true (channel-closed? (get-channel channels "b" "a")))
          )
        (is (nil? (get-active-interaction (get-monitor ab))))))))