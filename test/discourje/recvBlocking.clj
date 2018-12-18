(ns discourje.multi.recvBlocking
  (:require [clojure.test :refer :all]
            [clojure.core.async :refer :all]))

(def cha (chan))

(def futures (atom {}))
(defn recv! [c]
  (future (<!! c)))

(def xa (recv! cha))

(go (>! cha "asasasas"))


(def a (atom 1))

(while (= 1 @a)
  (do (println @a)
      (swap! a (fn [x] 1))))

(swap! a 2)
(go (>! cha "asasasas"))

(>!! cha "ss")
(close! cha)
(thread (println (<!! cha)))

(let [a (atom "function_scope")
      a2 (atom nil)
      b (atom nil)]
  (thread (do (reset! a (<!! cha))
              (reset! a2 (<!! cha))))
  (while (nil? @b)
    (Thread/sleep 1000)
    (when (compare-and-set! b @a 1)
    (reset! b ""))
    )
  @a)




(def recc (chan))
(defn rec [c]
  (let [v true
        a (atom nil)]
    (add-watch a nil (fn [key atom old-state new-state]
                 (prn "-- Atom Changed --")))
    (take! c (fn [x] (reset! a x)))
    (while (nil? @a)
      (Thread/sleep 1000)
      (reset! a nil)
      )
    @a))
(defn retu [a]
  (if (some? @a)
    @a
    (println (format "looping... %s" @a))))

(rec recc)
(go (>! recc "blaa"))
(close! recc)
(def c1 (chan))
(def atm (atom nil))
(println atm)
(take! c1 (fn [x] (reset! atm x)))
(go (>! c1 "aaa"))

(def ch (chan))
(def f (future (let [returnC (go (<! ch))]
                 (<!! returnC))))

(defn testf [callback]
  (let [f (future (go (callback (<! ch))))]
    (<!! @f)))

(println @f)
(testf (fn [x] (println x)))
(go (>! ch "this is the value"))

@f
(def at (atom nil))

(loop []
  (future
    (when-not (compare-and-set! at 0 1)
      (println "aaa")
      (recur))))

(reset! at 0)
(swap! at (fn [] 0))















;(def a (atom {:iterations 0 :value nil}))
(def a (atom nil))

(loop [b a]
  (if (nil? (:value))
    (do
      (println "yes nill")
      (recur a))
    (println "no longer null")))

(reset! @a "asdasdasd")
(reset! a nil)

(swap! @a (fn [] ("sdsdasdasdasd")))
(println (format "      sdsd                              %s" a))


(defn returnAtom [a]
  (println "still empty?")
  a)

(defn getAtomValue []
  (while (nil? @a)
    (Thread/sleep 100)
    (returnAtom a)))

(getAtomValue)