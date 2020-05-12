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

(def thrown?)

(use-fixtures :once defroles)

(defmacro no-throw [& body]
  `(try ~@body (catch RuntimeException e# e#)))

(defn failed? [thread]
  (= (type (a/<!! thread)) ExceptionInfo))

(defn not-failed?
  ([thread]
   (not (failed? thread)))
  ([thread & vals]
   (contains? (set vals) (a/<!! thread))))

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
  (let [clj (clj/>!! (clj/chan 1) "foo")
        dcj (dcj/>!! (dcj/chan 1) "foo")]
    (is (= clj dcj))))

(deftest chan-<!!-thread-tests
  (let [clj (clj/thread 3.14)
        dcj (dcj/thread 3.14)]
    (is (= (clj/<!! clj) (dcj/<!! dcj)))
    (is (= (clj/<!! clj) (dcj/<!! dcj)))))

(deftest chan->!!-<!!-thread-tests

  ;; Unbuffered

  (let [c (a/chan)
        t1 (a/thread (no-throw (a/>!! c "foo")))
        t2 (a/thread (no-throw (a/<!! c)))]
    (is (not-failed? t1))
    (is (not-failed? t2 "foo")))

  (let [m (a/monitor (s/end))
        c (a/chan (s/role ::alice) (s/role ::bob) m {})
        t1 (a/thread (no-throw (a/>!! c "foo")))
        t2 (a/thread (no-throw (a/<!! c)))]
    (is (failed? t1))
    (is (failed? t2)))

  (let [m (a/monitor (s/--> ::alice ::bob))
        c (a/chan (s/role ::alice) (s/role ::bob) m {})
        t1 (a/thread (no-throw (a/>!! c "foo")))
        t2 (a/thread (no-throw (a/<!! c)))]
    (is (not-failed? t1))
    (is (not-failed? t2 "foo")))

  (let [m (a/monitor (s/--> ::alice ::bob))
        c (a/chan (s/role ::alice) (s/role ::carol) m {})
        t1 (a/thread (no-throw (a/>!! c "foo")))
        t2 (a/thread (no-throw (a/<!! c)))]
    (is (failed? t1))
    (is (failed? t2)))

  ;; Buffered

  (let [c (a/chan 1)
        t1 (a/thread (no-throw (a/>!! c "foo")))
        t2 (a/thread (no-throw (a/<!! c)))]
    (is (not-failed? t1))
    (is (not-failed? t2 "foo")))

  (let [m (a/monitor (s/end))
        c (a/chan 1 (s/role ::alice) (s/role ::bob) m {})
        t1 (a/thread (no-throw (a/>!! c "foo")))
        t2 (a/thread (no-throw 3.14))]
    (is (failed? t1))
    (is (not-failed? t2 3.14)))

  (let [m (a/monitor (s/-->> ::alice ::bob))
        c (a/chan 1 (s/role ::alice) (s/role ::bob) m {})
        t1 (a/thread (no-throw (a/>!! c "foo")))
        t2 (a/thread (no-throw (a/<!! c)))]
    (is (not-failed? t1))
    (is (not-failed? t2 "foo")))

  (let [m (a/monitor (s/-->> ::alice ::bob))
        c (a/chan 1 (s/role ::alice) (s/role ::carol) m {})
        t1 (a/thread (no-throw (a/>!! c "foo")))
        t2 (a/thread (no-throw 3.14))]
    (is (failed? t1))
    (is (not-failed? t2 3.14))))

;;;;
;;;; CORE CONCEPTS: >!, <!, go
;;;;

;; TODO

;;;;
;;;; CORE CONCEPTS: alts!, alts!!, timeout
;;;;

(deftest chan-alts!!-tests
  (is (thrown? AssertionError (dcj/alts!! [])))

  ;; Unbuffered

  (is (= (clj/alts!! [(clj/chan)] :default -1)
         (dcj/alts!! [(dcj/chan)] :default -1)))

  (is (= (clj/alts!! [(clj/chan)] :default -1 :priority true)
         (dcj/alts!! [(dcj/chan)] :default -1 :priority true)))

  (let [c1 (a/chan)
        c2 (a/chan)
        t1 (a/thread (no-throw (a/alts!! [[c1 "foo"] [c2 "bar"]])))
        t2 (a/thread (no-throw (a/alts!! [c1 c2])))]
    (is (not-failed? t1))
    (is (not-failed? t2)))

  (let [c1 (a/chan)
        c2 (a/chan)
        t1 (a/thread (no-throw (a/alts!! [[c1 "foo"] c2])))
        t2 (a/thread (no-throw (a/alts!! [c1 [c2 "bar"]])))]
    (is (not-failed? t1))
    (is (not-failed? t2)))

  (let [m (a/monitor (s/alt (s/--> ::alice ::bob)
                            (s/--> ::bob ::alice)))
        c1 (a/chan (s/role ::alice) (s/role ::bob) m {})
        c2 (a/chan (s/role ::bob) (s/role ::alice) m {})
        t1 (a/thread (no-throw (a/alts!! [[c1 "foo"] [c2 "bar"]])))
        t2 (a/thread (no-throw (a/alts!! [c1 c2])))]
    (is (not-failed? t1))
    (is (not-failed? t2)))

  (let [m (a/monitor (s/alt (s/--> ::alice ::bob)
                            (s/--> ::bob ::alice)))
        c1 (a/chan (s/role ::alice) (s/role ::bob) m {})
        c2 (a/chan (s/role ::bob) (s/role ::alice) m {})
        t1 (a/thread (no-throw (a/alts!! [[c1 "foo"] c2])))
        t2 (a/thread (no-throw (a/alts!! [c1 [c2 "bar"]])))]
    (is (not-failed? t1))
    (is (not-failed? t2)))

  ;; Buffered

  (is (= (first (clj/alts!! [[(clj/chan 1) "foo"]]))
         (first (dcj/alts!! [[(dcj/chan 1) "foo"]]))))

  (is (= (clj/alts!! [(clj/chan 1)] :default -1)
         (dcj/alts!! [(dcj/chan 1)] :default -1)))

  (is (= (first (clj/alts!! [[(clj/chan 1) "foo"] [(clj/chan 1) "bar"]] :priority true))
         (first (dcj/alts!! [[(dcj/chan 1) "foo"] [(dcj/chan 1) "bar"]] :priority true))))

  (is (= (first (clj/alts!! [(clj/chan 1) [(clj/chan 1) "bar"]] :priority true))
         (first (dcj/alts!! [(dcj/chan 1) [(dcj/chan 1) "bar"]] :priority true))))

  (is (= (first (clj/alts!! [(clj/chan 1) (clj/chan 1)] :default -1 :priority true))
         (first (dcj/alts!! [(dcj/chan 1) (dcj/chan 1)] :default -1 :priority true))))

  (let [m (a/monitor (s/alt (s/-->> ::alice ::bob)
                            (s/-->> ::bob ::alice)))
        c1 (a/chan 1 (s/role ::alice) (s/role ::bob) m {})
        c2 (a/chan 1 (s/role ::bob) (s/role ::alice) m {})
        t1 (a/thread (no-throw (a/alts!! [[c1 "foo"] [c2 "bar"]])))
        t2 (a/thread (no-throw (a/alts!! [c1 c2])))]
    (is (not-failed? t1))
    (is (not-failed? t2)))

  ;; Unbuffered and buffered

  (let [m (a/monitor (s/alt (s/--> ::alice ::bob)
                            (s/-->> ::bob ::alice)))
        c1 (a/chan (s/role ::alice) (s/role ::bob) m {})
        c2 (a/chan 1 (s/role ::bob) (s/role ::alice) m {})
        t1 (a/thread (no-throw (a/alts!! [[c1 "foo"] c2])))
        t2 (a/thread (no-throw (a/alts!! [c1 [c2 "bar"]])))]
    (is (not-failed? t1))
    (is (not-failed? t2))))

(deftest chan->!!-thread-alts!!-tests

  ;; Unbuffered

  (let [c1 (a/chan)
        c2 (a/chan)
        t1 (a/thread (no-throw (a/>!! c1 "foo")))
        t2 (a/thread (no-throw 3.14))
        t3 (a/thread (no-throw (a/alts!! [c1 c2])))]
    (is (not-failed? t1))
    (is (not-failed? t2))
    (is (not-failed? t3 ["foo" c1])))

  (let [c1 (a/chan)
        c2 (a/chan)
        t1 (a/thread (no-throw 3.14))
        t2 (a/thread (no-throw (a/>!! c2 "foo")))
        t3 (a/thread (no-throw (a/alts!! [c1 c2])))]
    (is (not-failed? t1))
    (is (not-failed? t2))
    (is (not-failed? t3 ["foo" c2])))

  (let [m (a/monitor (s/alt (s/--> ::alice ::carol)
                            (s/--> ::bob ::carol)))
        c1 (a/chan (s/role ::alice) (s/role ::carol) m {})
        c2 (a/chan (s/role ::bob) (s/role ::carol) m {})
        t1 (a/thread (no-throw (a/>!! c1 "foo")))
        t2 (a/thread (no-throw 3.14))
        t3 (a/thread (no-throw (a/alts!! [c1 c2])))]
    (is (not-failed? t1))
    (is (not-failed? t2))
    (is (not-failed? t3 ["foo" c1])))

  (let [m (a/monitor (s/alt (s/--> ::alice ::carol)
                            (s/--> ::bob ::carol)))
        c1 (a/chan (s/role ::alice) (s/role ::carol) m {})
        c2 (a/chan (s/role ::bob) (s/role ::carol) m {})
        t1 (a/thread (no-throw 3.14))
        t2 (a/thread (no-throw (a/>!! c2 "foo")))
        t3 (a/thread (no-throw (a/alts!! [c1 c2])))]
    (is (not-failed? t1))
    (is (not-failed? t2))
    (is (not-failed? t3 ["foo" c2])))

  ;; Buffered

  (let [c1 (a/chan 1)
        c2 (a/chan 1)
        t1 (a/thread (no-throw (a/>!! c1 "foo")))
        t2 (a/thread (no-throw 3.14))
        t3 (a/thread (no-throw (a/alts!! [c1 c2])))]
    (is (not-failed? t1))
    (is (not-failed? t2))
    (is (not-failed? t3 ["foo" c1])))

  (let [c1 (a/chan 1)
        c2 (a/chan 1)
        t1 (a/thread (no-throw 3.14))
        t2 (a/thread (no-throw (a/>!! c2 "foo")))
        t3 (a/thread (no-throw (a/alts!! [c1 c2])))]
    (is (not-failed? t1))
    (is (not-failed? t2))
    (is (not-failed? t3 ["foo" c2])))

  (let [m (a/monitor (s/alt (s/-->> ::alice ::carol)
                            (s/-->> ::bob ::carol)))
        c1 (a/chan 1 (s/role ::alice) (s/role ::carol) m {})
        c2 (a/chan 1 (s/role ::bob) (s/role ::carol) m {})
        t1 (a/thread (no-throw (a/>!! c1 "foo")))
        t2 (a/thread (no-throw 3.14))
        t3 (a/thread (no-throw (a/alts!! [c1 c2])))]
    (is (not-failed? t1))
    (is (not-failed? t2))
    (is (not-failed? t3 ["foo" c1])))

  (let [m (a/monitor (s/alt (s/-->> ::alice ::carol)
                            (s/-->> ::bob ::carol)))
        c1 (a/chan 1 (s/role ::alice) (s/role ::carol) m {})
        c2 (a/chan 1 (s/role ::bob) (s/role ::carol) m {})
        t1 (a/thread (no-throw 3.14))
        t2 (a/thread (no-throw (a/>!! c2 "foo")))
        t3 (a/thread (no-throw (a/alts!! [c1 c2])))]
    (is (not-failed? t1))
    (is (not-failed? t2))
    (is (not-failed? t3 ["foo" c2])))

  ;; Unbuffered and buffered

  (let [c1 (a/chan)
        c2 (a/chan 1)
        t1 (a/thread (no-throw (a/>!! c1 "foo")))
        t2 (a/thread (no-throw 3.14))
        t3 (a/thread (no-throw (a/alts!! [c1 c2])))]
    (is (not-failed? t1))
    (is (not-failed? t2))
    (is (not-failed? t3 ["foo" c1])))

  (let [c1 (a/chan)
        c2 (a/chan 1)
        t1 (a/thread (no-throw 3.14))
        t2 (a/thread (no-throw (a/>!! c2 "foo")))
        t3 (a/thread (no-throw (a/alts!! [c1 c2])))]
    (is (not-failed? t1))
    (is (not-failed? t2))
    (is (not-failed? t3 ["foo" c2])))

  (let [m (a/monitor (s/alt (s/--> ::alice ::carol)
                            (s/-->> ::bob ::carol)))
        c1 (a/chan (s/role ::alice) (s/role ::carol) m {})
        c2 (a/chan 1 (s/role ::bob) (s/role ::carol) m {})
        t1 (a/thread (no-throw (a/>!! c1 "foo")))
        t2 (a/thread (no-throw 3.14))
        t3 (a/thread (no-throw (a/alts!! [c1 c2])))]
    (is (not-failed? t1))
    (is (not-failed? t2))
    (is (not-failed? t3 ["foo" c1])))

  (let [m (a/monitor (s/alt (s/--> ::alice ::carol)
                            (s/-->> ::bob ::carol)))
        c1 (a/chan (s/role ::alice) (s/role ::carol) m {})
        c2 (a/chan 1 (s/role ::bob) (s/role ::carol) m {})
        t1 (a/thread (no-throw 3.14))
        t2 (a/thread (no-throw (a/>!! c2 "foo")))
        t3 (a/thread (no-throw (a/alts!! [c1 c2])))]
    (is (not-failed? t1))
    (is (not-failed? t2))
    (is (not-failed? t3 ["foo" c2]))))

(deftest chan-<!!-thread-alts!!-tests

  ;; Unbuffered

  (let [c1 (a/chan)
        c2 (a/chan)
        t1 (a/thread (no-throw (a/alts!! [[c1 "foo"] [c2 "bar"]])))
        t2 (a/thread (no-throw (a/<!! c1)))
        t3 (a/thread (no-throw 3.14))]
    (is (not-failed? t1 [true c1]))
    (is (not-failed? t2 "foo"))
    (is (not-failed? t3)))

  (let [c1 (a/chan)
        c2 (a/chan)
        t1 (a/thread (no-throw (a/alts!! [[c1 "foo"] [c2 "bar"]])))
        t2 (a/thread (no-throw 3.14))
        t3 (a/thread (no-throw (a/<!! c2)))]
    (is (not-failed? t1 [true c2]))
    (is (not-failed? t2))
    (is (not-failed? t3 "bar")))

  (let [m (a/monitor (s/alt (s/--> ::alice ::bob)
                            (s/--> ::alice ::carol)))
        c1 (a/chan (s/role ::alice) (s/role ::bob) m {})
        c2 (a/chan (s/role ::alice) (s/role ::carol) m {})
        t1 (a/thread (no-throw (a/alts!! [[c1 "foo"] [c2 "bar"]])))
        t2 (a/thread (no-throw (a/<!! c1)))
        t3 (a/thread (no-throw 3.14))]
    (is (not-failed? t1 [true c1]))
    (is (not-failed? t2 "foo"))
    (is (not-failed? t3)))

  (let [m (a/monitor (s/alt (s/--> ::alice ::bob)
                            (s/--> ::alice ::carol)))
        c1 (a/chan (s/role ::alice) (s/role ::bob) m {})
        c2 (a/chan (s/role ::alice) (s/role ::carol) m {})
        t1 (a/thread (no-throw (a/alts!! [[c1 "foo"] [c2 "bar"]])))
        t2 (a/thread (no-throw 3.14))
        t3 (a/thread (no-throw (a/<!! c2)))]
    (is (not-failed? t1 [true c2]))
    (is (not-failed? t2))
    (is (not-failed? t3 "bar")))

  ;; Buffered

  (let [c1 (a/chan 1)
        c2 (a/chan 1)
        t1 (a/thread (no-throw (a/alts!! [[c1 "foo"] [c2 "bar"]] :priority true)))
        t2 (a/thread (no-throw (a/<!! c1)))
        t3 (a/thread (no-throw 3.14))]
    (is (not-failed? t1 [true c1]))
    (is (not-failed? t2 "foo"))
    (is (not-failed? t3)))

  (let [m (a/monitor (s/alt (s/-->> ::alice ::bob)
                            (s/-->> ::alice ::carol)))
        c1 (a/chan 1 (s/role ::alice) (s/role ::bob) m {})
        c2 (a/chan 1 (s/role ::alice) (s/role ::carol) m {})
        t1 (a/thread (no-throw (a/alts!! [[c1 "foo"] [c2 "bar"]] :priority true)))
        t2 (a/thread (no-throw (a/<!! c1)))
        t3 (a/thread (no-throw 3.14))]
    (is (not-failed? t1 [true c1]))
    (is (not-failed? t2 "foo"))
    (is (not-failed? t3)))

  ;; Unbuffered and buffered

  (let [c1 (a/chan)
        c2 (a/chan 1)
        t1 (a/thread (no-throw (a/alts!! [[c1 "foo"] [c2 "bar"]])))
        t2 (a/thread (no-throw 3.14))
        t3 (a/thread (no-throw (a/<!! c2)))]
    (is (not-failed? t1 [true c2]))
    (is (not-failed? t2))
    (is (not-failed? t3 "bar")))

  (let [m (a/monitor (s/alt (s/--> ::alice ::bob)
                            (s/-->> ::alice ::carol)))
        c1 (a/chan (s/role ::alice) (s/role ::bob) m {})
        c2 (a/chan 1 (s/role ::alice) (s/role ::carol) m {})
        t1 (a/thread (no-throw (a/alts!! [[c1 "foo"] [c2 "bar"]])))
        t2 (a/thread (no-throw 3.14))
        t3 (a/thread (no-throw (a/<!! c2)))]
    (is (not-failed? t1 [true c2]))
    (is (not-failed? t2))
    (is (not-failed? t3 "bar"))))

(deftest chan-!!>-<!!-thread-alts!!-tests

  ;; Unbuffered

  (let [c1 (a/chan)
        c2 (a/chan)
        t1 (a/thread (no-throw (a/alts!! [[c1 "foo"] c2])))
        t2 (a/thread (no-throw (a/<!! c1)))
        t3 (a/thread (no-throw 3.14))]
    (is (not-failed? t1 [true c1]))
    (is (not-failed? t2 "foo"))
    (is (not-failed? t3)))

  (let [c1 (a/chan)
        c2 (a/chan)
        t1 (a/thread (no-throw (a/alts!! [[c1 "foo"] c2])))
        t2 (a/thread (no-throw 3.14))
        t3 (a/thread (no-throw (a/>!! c2 "bar")))]
    (is (not-failed? t1 ["bar" c2]))
    (is (not-failed? t2))
    (is (not-failed? t3)))

  (let [m (a/monitor (s/alt (s/--> ::alice ::bob)
                            (s/--> ::alice ::carol)))
        c1 (a/chan (s/role ::alice) (s/role ::bob) m {})
        c2 (a/chan (s/role ::alice) (s/role ::carol) m {})
        t1 (a/thread (no-throw (a/alts!! [[c1 "foo"] c2])))
        t2 (a/thread (no-throw (a/<!! c1)))
        t3 (a/thread (no-throw 3.14))]
    (is (not-failed? t1 [true c1]))
    (is (not-failed? t2 "foo"))
    (is (not-failed? t3)))

  (let [m (a/monitor (s/alt (s/--> ::alice ::bob)
                            (s/--> ::alice ::carol)))
        c1 (a/chan (s/role ::alice) (s/role ::bob) m {})
        c2 (a/chan (s/role ::alice) (s/role ::carol) m {})
        t1 (a/thread (no-throw (a/alts!! [[c1 "foo"] c2])))
        t2 (a/thread (no-throw 3.14))
        t3 (a/thread (no-throw (a/>!! c2 "bar")))]
    (is (not-failed? t1 ["bar" c2]))
    (is (not-failed? t2))
    (is (not-failed? t3)))

  ;; Buffered

  (let [c1 (a/chan 1)
        c2 (a/chan 1)
        t1 (a/thread (no-throw (a/alts!! [[c1 "foo"] c2])))
        t2 (a/thread (no-throw (a/<!! c1)))
        t3 (a/thread (no-throw 3.14))]
    (is (not-failed? t1 [true c1]))
    (is (not-failed? t2 "foo"))
    (is (not-failed? t3)))

  (let [m (a/monitor (s/alt (s/-->> ::alice ::bob)
                            (s/-->> ::alice ::carol)))
        c1 (a/chan 1 (s/role ::alice) (s/role ::bob) m {})
        c2 (a/chan 1 (s/role ::alice) (s/role ::carol) m {})
        t1 (a/thread (no-throw (a/alts!! [[c1 "foo"] c2])))
        t2 (a/thread (no-throw (a/<!! c1)))
        t3 (a/thread (no-throw 3.14))]
    (is (not-failed? t1 [true c1]))
    (is (not-failed? t2 "foo"))
    (is (not-failed? t3)))

  ;; Unbuffered and buffered

  (let [c1 (a/chan)
        c2 (a/chan 1)
        t1 (a/thread (no-throw (a/alts!! [[c1 "foo"] c2])))
        t2 (a/thread (no-throw (a/<!! c1)))
        t3 (a/thread (no-throw 3.14))]
    (is (not-failed? t1 [true c1]))
    (is (not-failed? t2 "foo"))
    (is (not-failed? t3)))

  (let [c1 (a/chan)
        c2 (a/chan 1)
        t1 (a/thread (no-throw (a/alts!! [[c1 "foo"] c2])))
        t2 (a/thread (no-throw 3.14))
        t3 (a/thread (no-throw (a/>!! c2 "bar")))]
    (is (not-failed? t1 ["bar" c2]))
    (is (not-failed? t2))
    (is (not-failed? t3)))

  (let [m (a/monitor (s/alt (s/--> ::alice ::bob)
                            (s/-->> ::alice ::carol)))
        c1 (a/chan (s/role ::alice) (s/role ::bob) m {})
        c2 (a/chan 1 (s/role ::alice) (s/role ::carol) m {})
        t1 (a/thread (no-throw (a/alts!! [[c1 "foo"] c2])))
        t2 (a/thread (no-throw (a/<!! c1)))
        t3 (a/thread (no-throw 3.14))]
    (is (not-failed? t1 [true c1]))
    (is (not-failed? t2 "foo"))
    (is (not-failed? t3)))

  (let [m (a/monitor (s/alt (s/--> ::alice ::bob)
                            (s/-->> ::alice ::carol)))
        c1 (a/chan (s/role ::alice) (s/role ::bob) m {})
        c2 (a/chan 1 (s/role ::alice) (s/role ::carol) m {})
        t1 (a/thread (no-throw (a/alts!! [[c1 "foo"] c2])))
        t2 (a/thread (no-throw 3.14))
        t3 (a/thread (no-throw (a/>!! c2 "bar")))]
    (is (not-failed? t1 ["bar" c2]))
    (is (not-failed? t2))
    (is (not-failed? t3))))

(deftest <!!-timeout-tests
  (is (= (clj/<!! (clj/timeout 100))
         (dcj/<!! (dcj/timeout 100)))))

;;;;
;;;; CORE CONCEPTS: dropping-buffer, sliding-buffer
;;;;

(deftest chan->!!-<!!-dropping-buffer-tests
  (let [clj-c (clj/chan (clj/dropping-buffer 1))
        dcj-c (dcj/chan (dcj/dropping-buffer 1))]
    (clj/>!! clj-c 0)
    (clj/>!! clj-c 1)
    (dcj/>!! dcj-c 0)
    (dcj/>!! dcj-c 1)
    (is (= (clj/<!! clj-c) (dcj/<!! dcj-c)))))

(deftest chan->!!-<!!-sliding-buffer-tests
  (let [clj-c (clj/chan (clj/sliding-buffer 1))
        dcj-c (dcj/chan (dcj/sliding-buffer 1))]
    (clj/>!! clj-c 0)
    (clj/>!! clj-c 1)
    (dcj/>!! dcj-c 0)
    (dcj/>!! dcj-c 1)
    (is (= (clj/<!! clj-c) (dcj/<!! dcj-c)))))

;;;;
;;;; CORE CONCEPTS: core.async/examples/walkthrough.clj
;;;;

(deftest walkthrough

  ;; chan, close!

  (a/chan)
  (is true)

  (a/chan 10)
  (is true)

  (let [c (a/chan)]
    (a/close! c))
  (is true)

  ;; >!!, <!!, thread

  (let [m (a/monitor (s/cat (s/-->> ::alice ::bob)
                            (s/close ::alice ::bob)))
        c (a/chan 10 (s/role ::alice) (s/role ::bob) m {})]
    (a/>!! c "hello")
    (assert (= "hello" (a/<!! c)))
    (a/close! c))
  (is true)

  (let [m (a/monitor (s/cat (s/--> ::alice ::bob)
                            (s/close ::alice ::bob)))
        c (a/chan (s/role ::alice) (s/role ::bob) m {})]
    (a/thread (a/>!! c "hello"))
    (assert (= "hello" (a/<!! c)))
    (a/close! c))
  (is true)

  ;; >!, <!, go

  ;(let [c (chan)]
  ;  (go (>! c "hello"))
  ;  (assert (= "hello" (<!! (go (<! c)))))
  ;  (close! c))

  ;; alts!, alts!!, timeout

  (let [c1 (a/chan)
        c2 (a/chan)]
    (a/thread (while true
                (let [[v ch] (a/alts!! [c1 c2])]
                  (println "Read" v "from" ch))))
    (a/>!! c1 "hi")
    (a/>!! c2 "there"))
  (is true)

  ;(let [c1 (chan)
  ;      c2 (chan)]
  ;  (go (while true
  ;        (let [[v ch] (alts! [c1 c2])]
  ;          (println "Read" v "from" ch))))
  ;  (go (>! c1 "hi"))
  ;  (go (>! c2 "there")))

  ;(let [n 1000
  ;      cs (repeatedly n chan)
  ;      begin (System/currentTimeMillis)]
  ;  (doseq [c cs] (go (>! c "hi")))
  ;  (dotimes [i n]
  ;    (let [[v c] (alts!! cs)]
  ;      (assert (= "hi" v))))
  ;  (println "Read" n "msgs in" (- (System/currentTimeMillis) begin) "ms"))

  (let [t (a/timeout 100)
        begin (System/currentTimeMillis)]
    (a/<!! t)
    (println "Waited" (- (System/currentTimeMillis) begin)))
  (is true)

  (let [c (a/chan)
        begin (System/currentTimeMillis)]
    (a/alts!! [c (a/timeout 100)])
    (println "Gave up after" (- (System/currentTimeMillis) begin)))
  (is true)

  ;; dropping-buffer, sliding-buffer

  (a/chan (a/dropping-buffer 10))
  (is true)

  (a/chan (a/sliding-buffer 10))
  (is true))
