(ns discourje.twoBuyerProtocol.pipes.util
  (:require [clojure.core.async :as async :refer :all]))

;define a message record, only with content param at this moment
(defrecord message [tag content])

(defprotocol sequencePipeline
  (createPipeline [this to filter from] ))

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
