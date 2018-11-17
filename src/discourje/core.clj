(ns discourje.core
  (:require [clojure.core.async :as async :refer :all]
            [clojure.core :refer :all]))

(defprotocol messenger
  (putInput         ;put data or function on input channel
    [this data])
  (consumeInput     ;consume input
    [this])
  (takeOutput       ;take data or function from output
    [this]))

(defmacro sendOffData
  "takes a function and prepends a quote at the front to delay evaluation" ;overkill since data should not be evaluated anyway!!
  [f]
  `'~f)

(defmacro sendOffFunction
  "takes a function and prepends a quote at the front to delay evaluation, includes arity to support 1 argument(maybe more in future)"
  ([f]`'(~f))
  ([f value] `(~f ~value)))

(defn changeStateByEval
  "Swaps participant tag(:input, :state, :output) value by executing function on thread and setting result in tag"
  [participant function tag]
  (println (format "changeStateByEVAL: %s" (clojure.string/upper-case (str function)))) ;easy for debugging
  (swap! participant assoc tag (eval function)))

(defn changeStateByData
  "Swaps participant tag(:input, :state, :output) value by setting value on thread and setting result in tag"
  [participant data tag]
  (println (format "changeStateByDATA: %s" (clojure.string/upper-case (str data)))) ;easy for debugging
  (swap! participant assoc tag data))

(defn putMessage
  "Puts message on the channel, non-blocking"
  [channel message]
  (println (format "setting message %s" message))
  (go (>! channel message)))

(defn blockingTakeMessage
  "Takes message from the channel, blocking"
  [channel]
  (<!! channel))

(defn processInput
  "Consumes input from FROM and sends to input TO & more"
  ([from to]
  (consumeInput from)
  (putInput to (takeOutput from)))
  ;arity overload to support multiple receivers for the same data
  ([from to & more]
   (consumeInput from)
   (let [value (takeOutput from)]
     (putInput to value)
     (when (not (= '(nil) more))
       (for [receiver more] (putInput receiver value))))))

(defn sendInput
  "Sends input from FROM to TO"
  [data from to & more]
  (println data)
  (putInput from data)
  (processInput from to more))

(defn inputToState
  "moves input to state"
  [participant]
  (changeStateByData participant (blockingTakeMessage (:input @participant)) :state))

(defn branch
  "branch by function on input of given participant. Branch will also first move input -> state and then evaluate"
  ([participant function]
  (inputToState participant)
  (eval (function (:state @participant))))
  ([participant function trueBranch falseBranch]
   (if (branch participant function)
    trueBranch falseBranch)))

(defrecord participant [input output state]
  messenger
  (putInput [this data] (putMessage input data))
  (consumeInput [this] (putMessage (:output @this) (:state (changeStateByEval this (blockingTakeMessage (:input @this)) :state))))
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

(defmacro sendOffConsumingInput
  "takes a function and prepends a quote at the front to delay evaluation, consumes data on Input channel of the given participant"
  ([f participant]
   `(~f (blockingTakeMessage (:input (deref ~participant))))))