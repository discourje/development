(ns discourje.core.async.async
  (:require [clojure.core.async]
            [discourje.core.async.channels :refer :all])
  ;(use [discourje.core.async.generator :only [getDistinctRoles]])
  (:import (clojure.lang Seqable))
  )
(load "generator")

(defprotocol sendable
  (get-label [this])
  (get-content [this]))

(defprotocol transportable
  (get-provider [this])
  (get-consumer [this])
  (get-chan [this]))

(defprotocol edge
  (get-source-state [this])
  (get-sink-state [this])
  (get-action-label [this]))

(defprotocol stateful
  (get-id [this])
  (get-input-transitions [this])
  (get-output-transitions [this]))

(defprotocol interactable
  (get-action [this])
  (get-sender [this])
  (get-receiver [this]))

(defprotocol protocolable
  (get-interactions [this]))

(defrecord message [label content]
  sendable
  (get-label [this] label)
  (get-content [this] content))

(defrecord channel [sender receiver chan]
  transportable
  (get-provider [this] sender)
  (get-consumer [this] receiver)
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
  (get-output-transitions [this] (get-transitions-by-source id transitions)))

(defrecord interaction [action sender receiver]
  interactable
  (get-action [this] action)
  (get-sender [this] sender)
  (get-receiver [this] receiver))

(defrecord protocol [interactions]
  protocolable
  (get-interactions [this] interactions))

(defn -> [action sender receiver]
  "Creates an interaction object specifying sending action from sender to receiver."
  (->interaction action sender receiver))

(defn create-protocol [interactions]
  "Generate protocol based on interactions"
  (->protocol interactions))

(defn generateMonitor [protocol]
  (let [roles (getDistinctRoles protocol)]))

(defn- findAllRoles
  "List all sender and receivers in the protocol"
  [protocol result]
  (let [result2 (flatten (vec (conj result [])))]
    (conj result2
          (flatten
            (for [element protocol]
              (cond
                ;(instance? recursion element)
                ;(flatten (vec (conj result2 (findAllParticipants (:protocol element) result2))))
                ;(instance? choice element)
                ;(let [trueResult (findAllParticipants (:trueBranch element) result2)
                ;      falseResult (findAllParticipants (:falseBranch element) result2)]
                ;  (if (not (nil? trueResult))
                ;    (flatten (vec (conj result2 trueResult)))
                ;    (flatten (vec (conj result2 falseResult)))))
                (satisfies? interactable element)
                (do
                  (if (instance? Seqable (get-receiver element))
                    (conj result2 (flatten (get-receiver element)) (get-sender element))
                    (conj result2 (get-receiver element) (get-sender element))))))))))