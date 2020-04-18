(ns discourje.core.async-tests
  (:require [clojure.test :refer :all]
            [discourje.core.async :as a]
            [discourje.spec :as s]))

(alias 'clj 'clojure.core.async)
(alias 'dcj 'discourje.core.async)

(defn defroles [f]
  (s/defrole ::alice "alice")
  (s/defrole ::bob "bob")
  (s/defrole ::carol "carol")
  (s/defrole ::dave "dave")
  (f))

(defroles (fn [] true))

(use-fixtures :once defroles)

;(defmacro unsafe [& body]
;  `(try ~@body (catch Exception ~'_)))

;;;;
;;;; CORE CONCEPTS: chan, close!
;;;;

(deftest chan-close!-tests
  (let [clj (clj/close! (clj/chan))
        dcj (dcj/close! (dcj/chan))]
    (is (= clj dcj)))

  (let [clj (clj/close! (clj/chan 5))
        dcj (dcj/close! (dcj/chan 5))]
    (is (= clj dcj)))

  (let [m (a/monitor (s/close ::alice ::bob))
        c (a/chan (s/role ::alice) (s/role ::bob) m {})]
    (a/close! c)
    (is (thrown? RuntimeException (a/close! c))))

  (let [m (a/monitor (s/close ::alice ::bob))
        c (a/chan 5 (s/role ::alice) (s/role ::bob) m {})]
    (a/close! c)
    (is (thrown? RuntimeException (a/close! c)))))

;;;;;
;;;;; put! and take!
;;;;;
;
;(deftest put!-take!-tests
;  (let [m (a/monitor (s/--> ::alice ::bob))
;        c (a/chan (s/role ::alice) (s/role ::bob) m)]
;    (a/put! c 0)
;    (a/take! c identity)
;    (is true))
;
;  (let [m (a/monitor (s/* [] (s/--> ::alice ::bob)))
;        c (a/chan (s/role ::alice) (s/role ::bob) m)]
;    (a/put! c 0)
;    (a/take! c identity)
;    (a/put! c 0)
;    (a/take! c identity)
;    (a/put! c 0)
;    (a/take! c identity)
;    (is true))
;
;  (let [m (a/monitor (s/-->> ::alice ::bob))
;        c (a/chan 1 (s/role ::alice) (s/role ::bob) m)]
;    (a/put! c 0)
;    (a/take! c identity)
;    (is true))
;
;  (let [m (a/monitor (s/-->> ::alice ::bob))
;        c (a/chan 1 (s/role ::alice) (s/role ::bob) m)]
;    (a/put! c 0)
;    (a/take! c identity)
;    (is true))
;
;  (let [m (a/monitor (s/* [] (s/-->> ::alice ::bob)))
;        c (a/chan 1 (s/role ::alice) (s/role ::bob) m)]
;    (a/put! c 0)
;    (a/take! c identity)
;    (a/put! c 0)
;    (a/take! c identity)
;    (a/put! c 0)
;    (a/take! c identity)
;    (is true)))
;
;;;;;
;;;;; >!!, <!!, and thread
;;;;;
;
;(deftest >!!-<!!-thread-tests
;
;  ;; Buffered
;
;  (let [m (a/monitor (s/-->> ::alice ::bob))
;        c (a/chan 1 (s/role ::alice) (s/role ::bob) m)]
;    (a/>!! c 0)
;    (a/<!! c)
;    (is true)
;    (is (thrown? RuntimeException (a/>!! c 0))))
;
;  (let [m (a/monitor [(s/-->> ::alice ::bob)
;                      (s/-->> ::alice ::bob)
;                      (s/-->> ::alice ::bob)])
;        c (a/chan 1 (s/role ::alice) (s/role ::bob) m)]
;    (a/>!! c 0)
;    (a/<!! c)
;    (is true)
;    (a/>!! c 0)
;    (a/<!! c)
;    (is true)
;    (a/>!! c 0)
;    (a/<!! c)
;    (is true))
;
;  (let [m (a/monitor (s/parallel (s/-->> ::alice ::bob)
;                                 (s/-->> ::alice ::bob)
;                                 (s/-->> ::alice ::bob)))
;        c (a/chan 3 (s/role ::alice) (s/role ::bob) m)]
;    (a/>!! c 0)
;    (a/<!! c)
;    (is true)
;    (a/>!! c 0)
;    (a/<!! c)
;    (is true)
;    (a/>!! c 0)
;    (a/<!! c)
;    (is true)
;    (is (thrown? RuntimeException (a/>!! c 0))))
;
;  (let [m (a/monitor (s/parallel (s/-->> ::alice ::bob)
;                                 (s/-->> ::alice ::bob)
;                                 (s/-->> ::alice ::bob)))
;        c (a/chan 3 (s/role ::alice) (s/role ::bob) m)
;        t1 (a/thread "alice"
;                     (a/>!! c 0)
;                     (a/>!! c 0)
;                     (a/>!! c 0)
;                     (is true)
;                     (is (thrown? RuntimeException (a/>!! c 0))))
;        t2 (a/thread "bob"
;                     (a/<!! c)
;                     (a/<!! c)
;                     (a/<!! c))]
;    (a/<!! t1)
;    (a/<!! t2))
;
;  ;; Unbuffered
;
;  (let [m (a/monitor (s/--> ::alice ::bob))
;        c (a/chan 0 (s/role ::alice) (s/role ::bob) m)
;        t1 (a/thread (a/>!! c 0)
;                     (is true)
;                     (is (thrown? RuntimeException (a/>!! c 0))))
;        t2 (a/thread (a/<!! c)
;                     (unsafe (a/<!! c)))]
;    (a/<!! t1)
;    (a/<!! t2))
;
;  (let [m (a/monitor (s/parallel (s/--> ::alice ::bob)
;                                 (s/--> ::alice ::bob)
;                                 (s/--> ::alice ::bob)))
;        c (a/chan 0 (s/role ::alice) (s/role ::bob) m)
;        t1 (a/thread (a/>!! c 0)
;                     (a/>!! c 0)
;                     (a/>!! c 0)
;                     (is true)
;                     (is (thrown? RuntimeException (a/>!! c 0))))
;        t2 (a/thread (a/<!! c)
;                     (a/<!! c)
;                     (a/<!! c)
;                     (unsafe (a/<!! c)))]
;    (a/<!! t1)
;    (a/<!! t2))
;
;  (let [m (a/monitor [(s/--> ::alice ::bob)
;                      (s/--> ::alice ::bob)
;                      (s/--> ::alice ::bob)])
;        c (a/chan 0 (s/role ::alice) (s/role ::bob) m)
;        t1 (a/thread (a/>!! c 0)
;                     (a/>!! c 0)
;                     (a/>!! c 0)
;                     (is true)
;                     (is (thrown? RuntimeException (a/>!! c 0))))
;        t2 (a/thread (a/<!! c)
;                     (a/<!! c)
;                     (a/<!! c)
;                     (unsafe (a/<!! c)))]
;    (a/<!! t1)
;    (a/<!! t2)))