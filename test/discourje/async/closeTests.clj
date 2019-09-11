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
      (is true (channel-closed? ab))
      )
    (is (nil? (get-active-interaction (get-monitor ab))))
    )
  )

(deftest close-interaction-with-choice-and-closer-test
  (let [channels (add-infrastructure (interaction-with-choice-and-closer true))
        ab (get-channel channels "a" "b")]
    (set-logging-exceptions)
    (>!! ab (msg 0 0))
    (let [a->b (<!! ab 0)]
      (is (= (get-label a->b) 0))
      (close-channel! ab)
      (is true (channel-closed? ab))
      )
    (is (nil? (get-active-interaction (get-monitor ab))))
    )
  )

(deftest close-interaction-with-rec-and-closer-test
  (let [channels (add-infrastructure (interaction-with-rec-and-closer true))
        ab (get-channel channels "a" "b")]
    (set-logging-exceptions)
    (>!! ab (msg 0 0))
    (let [a->b (<!! ab 0)]
      (is (= (get-label a->b) 0))
      (close-channel! ab)
      (is true (channel-closed? ab))
      )
    (is (nil? (get-active-interaction (get-monitor ab))))
    )
  )

(deftest close-interaction-with-rec-and-choice-and-closer-test
  (let [channels (add-infrastructure (interaction-with-rec-and-choice-and-closer true))
        ab (get-channel channels "a" "b")]
    (set-logging-exceptions)
    (>!! ab (msg 0 0))
    (let [a->b (<!! ab 0)]
      (is (= (get-label a->b) 0))
      (close-channel! ab)
      (is true (channel-closed? ab))
      )
    (is (nil? (get-active-interaction (get-monitor ab))))
    )
  )

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
        (is true (channel-closed? ab))
        )

      )
    (is (nil? (get-active-interaction (get-monitor ab))))
    )
  )

(deftest close-interaction-with-parallel-and-closer-test
  (let [channels (add-infrastructure (interaction-with-parallel-and-closer true))
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
        (is true (channel-closed? ba)))
      )
    (is (nil? (get-active-interaction (get-monitor ab))))
    )
  )


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