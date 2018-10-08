(ns discourje.twoBuyerProtocol.pipes.util
  (:require [clojure.core.async :as async :refer :all]))

;define a message record, only with content param at this moment
(defrecord message [content])

;create a pipeline (sequence, no parallelism)
(defn setupPipeLineSequenceing
  "Creates a pipeline going from channel `from' to `to' exposing operations to filter"
  [from to filter]
  (pipeline 1 to filter from))

;protocol(interface) for filter
(defprotocol filter
  "filter some message"
  (filt [this message] "filer the message"))