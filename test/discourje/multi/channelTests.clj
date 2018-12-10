(ns discourje.multi.channelTests
  (:require [clojure.test :refer :all]
            [clojure.core.async :refer :all]))
(def a (chan))
(close! a)

(take! a (fn [x] (println x)) false)

(go (>! a "a"))
(defn getValue [x]
  x)
(take! a (fn [x] x))

(let [returnVal (atom nil)
      c (chan)]
  (put! c "value on channel")
  (take! c (fn [x] (reset! returnVal x)))
  @returnVal)

;this works
(def chann (chan))
(def returnVal (atom nil))
(take! chann (fn [x] (reset! returnVal x)))
(put! chann "bla")
(println returnVal)
;;;

(deftest nonBlockingTake
  (let [a (chan)
        returnV (atom nil)
        callback (fn [x] (reset! returnV x))]
    (go (>! a "hello"))
    (take! a callback)
    (is (= "hello" @returnV))
    (close! a)))

(for [x ["buyer1" "buyer2"]] (println x))

(defn contribute?
  "returns true when the received quote 50% or greater"
  [quote div]
  (println (format "received quote: %d and div: %d" quote div))
  (>= (* 100 (float (/ div quote))) 50))

(contribute? 20 10)

(def controlChannel (chan))
(let [c2 (chan)]
  (thread (while true
            (let [[v ch] (alts!! [controlChannel c2])]
              (println "Read" v "from" ch))))
  (>!! c2 "there"))

(go (>! controlChannel "a"))

(let [c2 (chan)]
  (let[[cC c2] (alts! [controlChannel c2])
       returnChan (take 1 cC)]
    (<!! returnChan)))

(close! controlChannel)
(go (>! controlChannel "hi"))


(defn recv [c1 values]
(<!!
  (let [vcount (count values)]
    (thread
      (loop [recvalue (<!! c1)
             reccount 1]
        (println "Read" recvalue)
        (if (= reccount vcount)
          (do (close! c1)
              :done)
          (recur (<!! c1) (inc reccount))))))))

(def c1 (chan))
(def values ["hi"])
(recv c1 values)
(go (>! c1 "aa"))

(defn waitForTake [ch]
  (go (loop []
        (when-some [val (<! ch)]
          val
          (recur)))))

(defn teszt [ch]
  (loop []
    (if-let [val (go (<! ch))]
      val
      (recur))))


(def ch (chan))
(teszt ch)
(go (>! ch "aa"))
(waitForTake ch)

(defn ab [c1]
(let [c2 (chan)]
  (thread (while true
            (let [[v ch] (alts!! [c1 c2])]
              (println "Read" v "from" ch))))
  ))
(def abc (chan))
(ab abc)
(go (>! abc "a"))


(defn waitFor [c]
  (let [returnv (atom nil)
        resetfn (fn [x] (do ((println x)
                              (reset! returnv x))))]
    (take! c resetfn)
    (while (nil? @returnv)
      (println @returnv))
    @returnv
    ))
(defn axx [c]
  (let [returnv (atom nil)]
    ;(take! c (fn [x] (reset! returnv x)))
    (go (reset! returnv (<! c)))
    (while (nil? @returnv))
    @returnv)
  )
(def waitForc (chan))
(axx waitForc)
(waitFor waitForc)
(go (>! waitForc "yes i waited"))


(defn waitWithAlt [Chan]
  (let [f (chan)]
    (alt!! Chan (<!! Chan)
           f ())))

(def at (atom nil))
(while (nil? @at) (println "at = nil"))
(swap! at (fn[x] "no longer nil"))
(reset! @at "asasas")