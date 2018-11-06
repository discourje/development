(ns discourje.core
  (:require [clojure.core.async :as async :refer :all]
            [clojure.core :refer :all]))

(defprotocol messenger
  (putInput         ;put data or function on input channel
    [this data])
  (consumeInput     ;consume input (shorthand for inputToThread & threadToOutput)
    [this]
    [this function])
  (takeOutput       ;take data or function from output
    [this]))

(defmacro sendOff
  "takes a function and prepends a quote at the from to delay evaluation"
  [f]
  `'~f)

(defn changeStateByEval
  "Swaps participant tag(:input, :state, :output) value by executing function on thread and setting result in tag"
  [participant function tag]
  (println (clojure.string/upper-case (str function))) ;easy for debugging
  (swap! participant assoc tag (eval function)))

(defn putMessage
  "Puts message on the channel, non-blocking"
  [channel message]
  (go (>! channel message)))

(defn blockingTakeMessage
  "Takes message from the channel, blocking"
  [channel]
  (<!! channel))

(defn processInput
  "Consumes input from FROM and sends to input TO"
  ([from to]
  (consumeInput from)
  (putInput to (takeOutput from)))
  ([function from to]
  ())
  )


(defn sendInput
  "Sends input from FROM to TO"
  [data from to]
  (putInput from data)
  (processInput from to))

(defrecord participant [input output state]
  messenger
  (putInput [this data] (putMessage input data))
  (consumeInput
    [this] (putMessage (:output @this) (:state (changeStateByEval this (blockingTakeMessage (:input @this)) :state)))
    [this function] (putMessage (:output @this) (:state (changeStateByEval this function :state)))
    )
  (takeOutput [this] (blockingTakeMessage output)))

(extend-type clojure.lang.Atom
  messenger
  (putInput [this data] (putMessage (:input @this) data))
  (consumeInput [this] (putMessage (:output @this) (:state (changeStateByEval this (blockingTakeMessage (:input @this)) :state))))
  (takeOutput [this] (blockingTakeMessage (:output @this))))

(defn createParticipant
  "Create a new participant, simulates constructor-like behavior"
  []
  (atom (->participant (chan) (chan) nil)))