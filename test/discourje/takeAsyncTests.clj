(ns discourje.takeAsyncTests
  (:require [clojure.test :refer :all]
            [clojure.core.async :refer :all]))
(def reference (atom ""))
(defn takeVal [channel]
  (take! channel
         (fn [x]
           (when (= x "hello 2")
             (println @reference))
             (reset! reference x))))

(let [r reference]
  (add-watch r nil
             (fn [key atom old-state new-state]
               (println new-state))))
(def c (chan))
(put! c "hello")
(put! c "hello 2")
(takeVal c)
(takeVal c)

(Thread/sleep 3000)
(let [r reference]
  (remove-watch r nil))
(close! c)

(defrecord a [])
(defrecord b [])
(def aTest (->a))
(def bTest (->b))
(println (= (type aTest) (type bTest)))

(subvec [0 1] 2)