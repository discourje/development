(ns discourje.core.async.async
  (:require [clojure.core.async]))

;load helper namespace files!
(load "interactions")
(load "channels")
(load "fsm")

(defprotocol sendable
  (get-label [this])
  (get-content [this]))

(defprotocol protocolable
  (get-interactions [this]))

(defrecord message [label content]
  sendable
  (get-label [this] label)
  (get-content [this] content))

(defrecord protocol [interactions]
  protocolable
  (get-interactions [this] interactions))

(defn -->> [action sender receiver]
  "Creates an interaction object specifying sending action from sender to receiver."
  (->interaction action sender receiver))

(defn create-protocol [interactions]
  "Generate protocol based on interactions"
  (->protocol interactions))

(defn generate-monitor [protocol]
  (let [roles (get-distinct-roles protocol)]))

