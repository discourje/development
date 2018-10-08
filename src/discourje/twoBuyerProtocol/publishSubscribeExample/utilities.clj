(ns discourje.twoBuyerProtocol.publishSubscribeExample.utilities
  (:require [clojure.test :refer :all]
            [clojure.core.async :as async :refer :all]))

;Define method to assign to publish on a channel
(defn publish "Define method to assign to publish on a channel"
  [channel] (pub channel :tag))

;Define a method to send a message with a tag on a channel
(defn send-with-tag "Send a message with a certain tag on a channel"
  [msg tag channel]
  (>!! channel {:msg msg :tag tag}))
;send nonblocking message on log channel
(defn go-with-tag "Send a message with a certain tag on a channel"
  [msg tag channel]
  (go (>! channel {:msg msg :tag tag})))

;create logging channel
(def logChannel (chan))
;loop through channel endlessly
(go (loop []
      (when-let [v (<! logChannel)]
        (println v)
        (recur))))
;log a message to the logchannel
(defn logMessage [message]
  "Log a message"
  (go (>! logChannel message)))

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