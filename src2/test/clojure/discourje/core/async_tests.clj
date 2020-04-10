(ns discourje.core.async-tests
  (:require [clojure.test :refer :all]
            [discourje.core.async :as a]
            [discourje.core.async.spec :as s]))

(defn defroles [f]
  (s/defrole ::alice "alice")
  (s/defrole ::bob "bob")
  (s/defrole ::carol "carol")
  (s/defrole ::dave "dave")
  (f))

(defroles (fn [] true))

(use-fixtures :once defroles)

(defmacro unsafe [& body]
  `(try ~@body (catch Exception ~'_)))

;;;;
;;;; put! and take!
;;;;

(deftest put!-take!-tests
  (let [m (s/monitor (s/--> ::alice ::bob))
        c (a/chan (s/role ::alice) (s/role ::bob) m)]
    (a/put! c 0)
    (a/take! c identity)
    (is true))

  (let [m (s/monitor (s/* [] (s/--> ::alice ::bob)))
        c (a/chan (s/role ::alice) (s/role ::bob) m)]
    (a/put! c 0)
    (a/take! c identity)
    (a/put! c 0)
    (a/take! c identity)
    (a/put! c 0)
    (a/take! c identity)
    (is true))

  (let [m (s/monitor (s/-->> ::alice ::bob))
        c (a/chan 1 (s/role ::alice) (s/role ::bob) m)]
    (a/put! c 0)
    (a/take! c identity)
    (is true))

  (let [m (s/monitor (s/-->> ::alice ::bob))
        c (a/chan 1 (s/role ::alice) (s/role ::bob) m)]
    (a/put! c 0)
    (a/take! c identity)
    (is true))

  (let [m (s/monitor (s/* [] (s/-->> ::alice ::bob)))
        c (a/chan 1 (s/role ::alice) (s/role ::bob) m)]
    (a/put! c 0)
    (a/take! c identity)
    (a/put! c 0)
    (a/take! c identity)
    (a/put! c 0)
    (a/take! c identity)
    (is true)))

;;;;
;;;; >!!, <!!, and thread
;;;;

(deftest >!!-<!!-thread-tests

  ;; Buffered

  (let [m (s/monitor (s/-->> ::alice ::bob))
        c (a/chan 1 (s/role ::alice) (s/role ::bob) m)]
    (a/>!! c 0)
    (a/<!! c)
    (is true)
    (is (thrown? RuntimeException (a/>!! c 0))))

  (let [m (s/monitor [(s/-->> ::alice ::bob)
                      (s/-->> ::alice ::bob)
                      (s/-->> ::alice ::bob)])
        c (a/chan 1 (s/role ::alice) (s/role ::bob) m)]
    (a/>!! c 0)
    (a/<!! c)
    (is true)
    (a/>!! c 0)
    (a/<!! c)
    (is true)
    (a/>!! c 0)
    (a/<!! c)
    (is true))

  (let [m (s/monitor (s/parallel (s/-->> ::alice ::bob)
                                 (s/-->> ::alice ::bob)
                                 (s/-->> ::alice ::bob)))
        c (a/chan 3 (s/role ::alice) (s/role ::bob) m)]
    (a/>!! c 0)
    (a/<!! c)
    (is true)
    (a/>!! c 0)
    (a/<!! c)
    (is true)
    (a/>!! c 0)
    (a/<!! c)
    (is true)
    (is (thrown? RuntimeException (a/>!! c 0))))

  (let [m (s/monitor (s/parallel (s/-->> ::alice ::bob)
                                 (s/-->> ::alice ::bob)
                                 (s/-->> ::alice ::bob)))
        c (a/chan 3 (s/role ::alice) (s/role ::bob) m)
        t1 (a/thread "alice"
                     (a/>!! c 0)
                     (a/>!! c 0)
                     (a/>!! c 0)
                     (is true)
                     (is (thrown? RuntimeException (a/>!! c 0))))
        t2 (a/thread "bob"
                     (a/<!! c)
                     (a/<!! c)
                     (a/<!! c))]
    (a/<!! t1)
    (a/<!! t2))

  ;; Unbuffered

  (let [m (s/monitor (s/--> ::alice ::bob))
        c (a/chan 0 (s/role ::alice) (s/role ::bob) m)
        t1 (a/thread (a/>!! c 0)
                     (is true)
                     (is (thrown? RuntimeException (a/>!! c 0))))
        t2 (a/thread (a/<!! c)
                     (unsafe (a/<!! c)))]
    (a/<!! t1)
    (a/<!! t2))

  (let [m (s/monitor (s/parallel (s/--> ::alice ::bob)
                                 (s/--> ::alice ::bob)
                                 (s/--> ::alice ::bob)))
        c (a/chan 0 (s/role ::alice) (s/role ::bob) m)
        t1 (a/thread (a/>!! c 0)
                     (a/>!! c 0)
                     (a/>!! c 0)
                     (is true)
                     (is (thrown? RuntimeException (a/>!! c 0))))
        t2 (a/thread (a/<!! c)
                     (a/<!! c)
                     (a/<!! c)
                     (unsafe (a/<!! c)))]
    (a/<!! t1)
    (a/<!! t2))

  (let [m (s/monitor [(s/--> ::alice ::bob)
                      (s/--> ::alice ::bob)
                      (s/--> ::alice ::bob)])
        c (a/chan 0 (s/role ::alice) (s/role ::bob) m)
        t1 (a/thread (a/>!! c 0)
                     (a/>!! c 0)
                     (a/>!! c 0)
                     (is true)
                     (is (thrown? RuntimeException (a/>!! c 0))))
        t2 (a/thread (a/<!! c)
                     (a/<!! c)
                     (a/<!! c)
                     (unsafe (a/<!! c)))]
    (a/<!! t1)
    (a/<!! t2)))