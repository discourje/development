(ns discourje.twoBuyerProtocol.pipes.simpleSetup
  (:require [clojure.core.async :as async :refer :all]))

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

(defn scribbleSequence
  "function to replicate scribble syntax, however we need a filter for now to only take interesting messages from the channels"
  [operation filter from to]
  (pipeline 1 to (filter filter) from)
  (go (>! from operation)))

(defn scribbleMultipleTo
  "function to replicate scribble syntax, however we need a filter for now to only take interesting messages from the channels"
  [operation filter from to & more]
  (pipeline 1 to (filter filter) from)
  (for [m more]
    ((pipeline 1 m (filter filter) from)))
  (go (>! from operation)))


(def greet (fn [x] (print x)))

(defn generateBook []
  (println "generating book")
  (str "Joy of Clojure"))

(linkBetween "heya" seller string? buyer1 greet)

(defn generateQuote []
  (println "generating random integer between 0 and 30")
  (+ (rand-int 30) 1))

(def generateQuoteDiv
    (fn [quote] (+ (rand-int quote) 1)))

(defn generateQuoteToChannels
  "Generate random qoute and put on buyer1 and buyer 2 channel"
  [buyer1 buyer2]
  (let [quote (+ (rand-int 30) 1)]
    (go (>! buyer1 quote))
    (go (>! buyer2 quote))))

(defn scribbleChoice [choice acceptPipe declinePipe]
  (println "chosing between accept or decline")
  (if (choice) acceptPipe declinePipe))

(def quoteDivHigherThan15 (fn [x] (< 15 x)))

(defn CreateTwoBuyerProtocol []
  (let [buyer1 (chan)
        buyer2 (chan)
        seller (chan)]
    (scribbleSequence (generateBook) string? buyer1 seller)
    (scribbleMultipleTo (generateQuote) number? seller buyer1 buyer2)
    (scribbleSequence generateQuoteDiv number? buyer1 buyer2)
    ;(scribbleChoice quoteDivHigherThan15
    ;                (
    ;                  (scribbleSequence (fn [] ("ok")) string? buyer2 seller)
    ;                  (scribbleSequence (fn [] ("Address:  test test")) string? buyer2 seller)
    ;                  (scribbleSequence (fn [] ("Date:  01-01-2018")) string? seller buyer2))
    ;                (scribbleSequence (fn [] ("quit")) string? buyer2 seller))
    ))