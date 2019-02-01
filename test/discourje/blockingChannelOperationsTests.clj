(ns discourje.blockingChannelOperationsTests
  (:require [clojure.test :refer :all]
            [clojure.core.async :refer :all]))


(def c (chan 1))
(def d (chan 1))

(defn validate [value]
  (do (println "validating") value))

(defn write [x callback]
  (do (>!! c (validate x))
      (callback)))

(defn read []
  (<!! c))

(defn singleWrite[]
  (thread (do
            (println (write "hello world" (fn [] (println "yes yes helloworld is written"))))
            (println (read))
            )))
(singleWrite)

(defn doubleWrite[]
  (thread (do
            (println (write "hello"(fn [] (println "yes yes hello is written"))))
            (println "hello written!")
            (println (write "world"(fn [] (println "yes yes world is written"))))
            (println "world written!")
            (println (read))
            )))
(doubleWrite)

(close! c)
