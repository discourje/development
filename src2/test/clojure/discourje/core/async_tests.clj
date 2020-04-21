(ns discourje.core.async-tests
  (:require [clojure.test :refer :all]
            [discourje.core.async :as a]
            [discourje.spec :as s])
  (:import (clojure.lang ExceptionInfo)))

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

(defmacro no-throw [& body]
  `(try ~@body (catch RuntimeException e# e#)))

(defn failed? [thread]
  (= (type (a/<!! thread)) ExceptionInfo))

(defn not-failed?
  ([thread]
   (not (failed? thread)))
  ([thread val]
   (= (a/<!! thread) val)))

;;;;
;;;; CORE CONCEPTS: chan, close!
;;;;

(deftest chan-close!-tests

  ;; Unbuffered

  (let [clj (clj/close! (clj/chan))
        dcj (dcj/close! (dcj/chan))]
    (is (= clj dcj)))

  (let [m (a/monitor (s/end))
        c (a/chan (s/role ::alice) (s/role ::bob) m {})]
    (is (thrown? RuntimeException (a/close! c))))

  (let [m (a/monitor (s/close ::alice ::bob))
        c (a/chan (s/role ::alice) (s/role ::bob) m {})]
    (a/close! c)
    (is true))

  ;; Buffered

  (let [clj (clj/close! (clj/chan 1))
        dcj (dcj/close! (dcj/chan 1))]
    (is (= clj dcj)))

  (let [m (a/monitor (s/end))
        c (a/chan 1 (s/role ::alice) (s/role ::bob) m {})]
    (is (thrown? RuntimeException (a/close! c))))

  (let [m (a/monitor (s/close ::alice ::bob))
        c (a/chan 1 (s/role ::alice) (s/role ::bob) m {})]
    (a/close! c)
    (is true)))

;;;;
;;;; CORE CONCEPTS: >!!, <!!, thread
;;;;

(deftest chan->!!-tests
  (let [clj (clj/>!! (clj/chan 1) 0)
        dcj (dcj/>!! (dcj/chan 1) 0)]
    (is (= clj dcj))))

(deftest chan-<!!-thread-tests
  (let [clj (clj/thread 0)
        dcj (dcj/thread 0)]
    (is (= (clj/<!! clj) (dcj/<!! dcj)))
    (is (= (clj/<!! clj) (dcj/<!! dcj)))))

(deftest chan->!!-<!!-thread-tests

  ;; Unbuffered

  (let [c (a/chan)
        t1 (a/thread (no-throw (a/>!! c 0)))
        t2 (a/thread (no-throw (a/<!! c)))]
    (is (not-failed? t1))
    (is (not-failed? t2 0)))

  (let [m (a/monitor (s/end))
        c (a/chan (s/role ::alice) (s/role ::bob) m {})
        t1 (a/thread (no-throw (a/>!! c 0)))
        t2 (a/thread (no-throw (a/<!! c)))]
    (is (failed? t1))
    (is (failed? t2)))

  (let [m (a/monitor (s/--> ::alice ::bob))
        c (a/chan (s/role ::alice) (s/role ::bob) m {})
        t1 (a/thread (no-throw (a/>!! c 0)))
        t2 (a/thread (no-throw (a/<!! c)))]
    (is (not-failed? t1))
    (is (not-failed? t2 0)))

  (let [m (a/monitor (s/--> ::alice ::bob))
        c (a/chan (s/role ::alice) (s/role ::carol) m {})
        t1 (a/thread (no-throw (a/>!! c 0)))
        t2 (a/thread (no-throw (a/<!! c)))]
    (is (failed? t1))
    (is (failed? t2)))

  ;; Buffered

  (let [c (a/chan 1)
        t1 (a/thread (no-throw (a/>!! c 0)))
        t2 (a/thread (no-throw (a/<!! c)))]
    (is (not-failed? t1))
    (is (not-failed? t2 0)))

  (let [m (a/monitor (s/end))
        c (a/chan 1 (s/role ::alice) (s/role ::bob) m {})
        t1 (a/thread (no-throw (a/>!! c 0)))
        t2 (a/thread (no-throw 0))]
    (is (failed? t1))
    (is (not-failed? t2 0)))

  (let [m (a/monitor (s/-->> ::alice ::bob))
        c (a/chan 1 (s/role ::alice) (s/role ::bob) m {})
        t1 (a/thread (no-throw (a/>!! c 0)))
        t2 (a/thread (no-throw (a/<!! c)))]
    (is (not-failed? t1))
    (is (not-failed? t2 0)))

  (let [m (a/monitor (s/-->> ::alice ::bob))
        c (a/chan 1 (s/role ::alice) (s/role ::carol) m {})
        t1 (a/thread (no-throw (a/>!! c 0)))
        t2 (a/thread (no-throw 0))]
    (is (failed? t1))
    (is (not-failed? t2 0))))

