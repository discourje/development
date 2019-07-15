(ns discourje.async.PeekTests
  (:require [clojure.test :refer :all]
            [clojure.core.async.impl.protocols :as bufs]
            [discourje.core.async :refer :all :as dcj]))

(deftest upon-construction-buffer-empty-test
  (let [c (clojure.core.async/chan 1)]
    (is (false? (buffer-full? c)))
    (clojure.core.async/close! c)))

(deftest after-put-buffer-full-test
  (let [c (clojure.core.async/chan 1)]
    (clojure.core.async/>!! c "1")
    (is (true? (buffer-full? c)))
    (clojure.core.async/close! c)))

(deftest upon-construction-nothing-in-buffer-test
  (let [c (clojure.core.async/chan 1)]
    (is (false? (something-in-buffer? c)))
    (clojure.core.async/close! c)))

(deftest after-put-something-in-buffer-test
  (let [c (clojure.core.async/chan 1)]
    (clojure.core.async/>!! c "1")
    (is (true? (something-in-buffer? c)))
    (clojure.core.async/close! c)))

(deftest after-put-something-in-buffer-but-not-full-test
  (let [c (clojure.core.async/chan 2)]
    (clojure.core.async/>!! c "1")
    (is (true? (something-in-buffer? c)))
    (is (false? (buffer-full? c)))
    (clojure.core.async/close! c)))
;
;(defn await-put [c v]
;  (loop [] (when (true? (buffer-full? c)) (recur)))
;  ())
;
;(defn- await-take-while [c label]
;  (if (something-in-buffer? c)
;    (let [value (peek-channel c)]
;      (= (:label value) label))
;    (do (while (false? (something-in-buffer? c)))
;        (await-take-while c label))))
;
;(defn await-take-loop [c label]
;  (loop []
;    (when (false? (something-in-buffer? c)) (recur)))
;  (let [value (peek-channel c)]
;    (= (:label value) label)))
;
;(def c (clojure.core.async/chan 2))
;(buffer-full? c)
;(something-in-buffer? c)
;(thread (>!! c {:label "hi" :content "this is content"}))
;(clojure.core.async/thread (do (println "yes yes" (await-take-while c "hi")))
;        (println (clojure.core.async/<!! c)))
;
;(clojure.core.async (do (println "yes yes" (await-take-loop c "hi")))
;        (println (clojure.core.async/<!! c)))
;
;(thread (println (peek-channel c)))
;(thread (println (clojure.core.async/<!! c)))
;(clojure.core.async/close! c)