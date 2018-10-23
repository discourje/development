(ns research.twoBuyerProtocol.pipes.simpleSetup
  (:require [clojure.core.async :as async :refer :all]))

(defn testCC [channel]
  (fn [] (go (<! channel)))
  (fn [] (go (<! channel))))
(testCC (chan))


(defn scribbleSequence
  "function to replicate scribble syntax, however we need a filter for now to only take interesting messages from the channels"
  [operation filter from to]
  (pipeline 1 to (filter filter) from)
  (fn [] (go (>! from operation))))

(defn scribbleMultipleTo
  "function to replicate scribble syntax, however we need a filter for now to only take interesting messages from the channels"
  [operation filter from to & more]
  (pipeline 1 to (filter filter) from)
  (for [m more]
    (pipeline 1 m (filter filter) from))
  (fn [] (go (>! from operation))))


(defn generateBook []
  (println "generating book")
  (str "Joy of Clojure"))

(defn generateQuote []
  (println "generating random integer between 0 and 30")
  (+ (rand-int 30) 1))

(def generateQuoteDiv
  (fn [quote]
    (+ (rand-int quote) 1)))

(defn quoteDivHigherThan15 [channel]
  (go (let [quote (<! channel)]
        (println (format "quote is %s" quote))
        (> 15 12))))

(defn CreateTwoBuyerProtocol []
  (let [buyer1 (chan)
        buyer2 (chan)
        seller (chan)]
    (scribbleSequence (generateBook) string? buyer1 seller)
    (scribbleMultipleTo (generateQuote) number? seller buyer1 buyer2)
    (scribbleSequence generateQuoteDiv number? buyer1 buyer2)
    (if (quoteDivHigherThan15 buyer2)
      ;(
        (scribbleSequence (fn [x] (str "ok")) string? buyer2 seller)
        ;(scribbleSequence (fn [x] (str "Address: test test")) string? buyer2 seller)
        ;(scribbleSequence (fn [x] (str "Date:  01-01-2018")) string? seller buyer2)
        ; )
      (scribbleSequence (fn [x] (str "quit")) string? buyer2 seller))))
(discourje.twoBuyerProtocol.pipes.simpleSetup/CreateTwoBuyerProtocol)


(defn scribbleChoice [choice acceptPipe declinePipe]
  (println "chosing between accept or decline")
  (if (choice) acceptPipe declinePipe))

(defn generateQuoteToChannels
  "Generate random qoute and put on buyer1 and buyer 2 channel"
  [buyer1 buyer2]
  (let [quote (+ (rand-int 30) 1)]
    (go (>! buyer1 quote))
    (go (>! buyer2 quote))))

(def buyer1 (chan))
(def buyer2 (chan))
(def seller (chan))

(pipeline 1 seller (filter string?) buyer1)

(go (>! buyer1 "hello from buyer1 to seller"))
(go (println (<! seller)))

(defn linkBetween
  "Sets up a pipeline between TO and FROM sending MESSAGE from FROM to TO exposing it to FILTER and calling OPERATION when received."
  [message to filter from operation]
  (pipeline 1 to (filter filter) from)
  (go (>! from message))
  (go-loop []
    (operation (<! to))))
(def greet (fn [x] (print x)))
(linkBetween "heya" seller string? buyer1 greet)
