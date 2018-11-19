(ns research.twoBuyerProtocol.pipes.util
  (:require [clojure.core.async :as async :refer :all]))

;define a message record, only with content param at this moment
(defrecord message [tag content])

(defprotocol sequencePipeline
  (createPipeline [this to filter from]))

;create a pipeline (sequence, no parallelism) "Creates a pipeline going from channel `from' to `to' exposing operations to filter"
(defrecord setupPipeLineSequencing [from to filter]
  sequencePipeline
  (createPipeline [this to filter from] (pipeline 1 to filter from)))

;protocol(interface) for filter
(defprotocol messageFilter
  "filter some message"
  (filt [this message] "filer the message"))

(defrecord stringFilter []
  messageFilter
  (filt [this message] (string? message)))

(defrecord tagFilter [tag]
  messageFilter
  (filt [this message] (= (:tag message) tag)))

(def bookCollection
  (vector "The Joy of Clojure" "Clojure Programming" "Mastering Clojure Macros"))

(defn putOnChannel
  "Put a new message with the given tag on a channel"
  [channel message tag]
  (go (>! channel (->message tag message))))


; Generate a new Java Date
(defn getDate "Generate a new date and increment an amount of days"
  [days]
  (let [cal (java.util.Calendar/getInstance)
        d (new java.util.Date)]
    (doto cal
      (.setTime d)
      (.add java.util.Calendar/DATE days)
      (.getTime))))

; Generate a new java date with a random amount of days incremented up to a specified range
(defn getRandomDate "Get a random date, in the future, up to a maximum range (inclusive)"
  [maxRange]
  (getDate (+ (rand-int maxRange) 1)))

; generate a (pseudo)random bool, use the random number generator between 0 and 2(exclusive) and check whether it is 1
(defn randomBoolean "Generate random boolean"
  []
  (= 1 (rand-int 2)))

; Close all channels given as arguments
(defn closeN! "Calls close! on n channels given as arguments since core.async does not have a close multiple channels function"
  [c & more]
  (apply close! c)
  (for [channel more] (apply close! channel)))