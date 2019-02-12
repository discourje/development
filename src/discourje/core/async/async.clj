(ns discourje.core.async.async
  (:require [clojure.core.async])
  (:import (clojure.lang Seqable))
  )

;load helper namespace files!
(load "interactions")
(load "channels")
;(load "fsm")
(load "monitoring")

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

(defn- link-interactions [protocol]
  (let [interactions (get-interactions protocol)
        linked-interactons []]

    )
  )

(defn generate-monitor [protocol]
  (let [roles (get-distinct-roles (get-interactions protocol))]
    (->monitor (link-interactions protocol) (generate-channels roles 1) (atom (first (get-interactions protocol))))))



;(defn get-transitions-in-protocol [protocol]
;  (interactions-to-transitions (get-interactions protocol)))
;
;(defn- generate-io-fsms
;  "Convert a protocol of interactions to IO enabled finite-state-machines local to each role."
;  [protocol]
;  (let [roles (get-distinct-roles (get-interactions protocol))]))

