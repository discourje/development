(ns discourje.multi.recvLooping
  (:require [clojure.test :refer :all]
            [clojure.core.async :refer :all]))

(def r (ref nil))
(println r)

(while (nil? @r)
  (println "r =  nil"))
(dosync (commute r (fn [] 1)))

(let [x (atom nil)
      f (memoize (println @x))]
  (add-watch x :watcher
             (fn [key atom old-state new-state]
               (prn "-- Atom Changed --")
               (prn "key" key)
               (prn "atom" atom)
               (prn "old-state" old-state)
               (prn "new-state" new-state)
               (f))
             )
  (reset! x "aa")
  )


(defn interruptible-sleep
  ([millis break-f] (interruptible-sleep millis break-f 100))
  ([millis break-f polling-millis]
   (if (> millis 0)
     (let [end (+ (System/currentTimeMillis) millis)]
       (loop []
         (let [now (System/currentTimeMillis)
               remaining-millis (- end now)]
           (if (and (> remaining-millis 0) (not (break-f)))
             (do
               (println "running")
               (if (>= remaining-millis 5)
                 (Thread/sleep (min polling-millis remaining-millis)))
               (recur))
             (- remaining-millis))))))))

(def a (atom nil))
(interruptible-sleep 100000000000 (fn [](some? @a)))
(println @a)
(reset! a "111")




(def one (atom nil))
(def a (atom 10))

(while (pos? @a)
  (do (println @a)
      (if (some? @one)
        (reset! a -1)
        (reset! a 30))))
(while (pos? @a) (do (println @a) (swap! a dec)))

(let [a ( at)]
  (println a)
  (println a))

(swap! one (fn [x] "sasdsdsdsdsdsdsd"))

(defn loopReady[a]
  (loop []
    (when (= :ready (:status a))
      (do (println (:status a))
          (recur)))))

(defn loopPending [a]
  (loop []
    (when (= :pending (:status a))
      (do println (:status a)
          (recur)))))

(let [a at]
  (loopReady a)
  (loopPending a)
  @a)

