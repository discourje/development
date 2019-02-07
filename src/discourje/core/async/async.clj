(ns discourje.core.async.async
  (:require [clojure.core.async]
            [discourje.core.async.channels :refer :all]))

(defprotocol sendable
  (get-label [this])
  (get-content [this]))

(defprotocol transportable
  (get-sender [this])
  (get-receiver [this])
  (get-chan [this]))

(defprotocol edge
  (get-source-state [this])
  (get-sink-state [this])
  (get-action-label [this]))

(defprotocol stateful
  (get-id [this])
  (get-input-transitions [this])
  (get-output-transitions [this]))

(defrecord message [label content]
  sendable
  (get-label [this] label)
  (get-content [this] content))

(defrecord channel [sender receiver chan]
  transportable
  (get-sender [this] sender)
  (get-receiver [this] receiver)
  (get-chan [this] chan))

(defrecord transition [source sink label]
  edge
  (get-source-state [this] source)
  (get-sink-state [this] sink)
  (get-action-label [this] label))

(defrecord node [id transitions active]
  stateful
  (get-id [this] id)
  (get-input-transitions [this] (get-transitions-by-sink id transitions))
  (get-output-transitions[this] (get-transitions-by-source id transitions)))

