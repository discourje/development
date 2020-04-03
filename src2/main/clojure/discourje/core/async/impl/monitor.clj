(ns discourje.core.async.impl.monitor
  (require [discourje.core.async.logging :refer :all]
           [discourje.core.async.impl.dsl.abstract :refer :all]))

(defprotocol sendable
  (is-valid-sendable? [this monitor sender receivers message])
  (apply-sendable! [this pre-swap-interaction active-interaction monitor sender receivers message])
  (get-sendable [this monitor sender receivers message]))

(defprotocol receivable
  (is-multicast? [this monitor message])
  (is-valid-receivable? [this monitor sender receivers message])
  (apply-receivable! [this pre-swap-interaction active-interaction monitor sender receivers message])
  (get-receivable [this monitor sender receivers message]))

(defprotocol terminatable
  (is-valid-closable? [this monitor sender receiver])
  (apply-closable! [this pre-swap-interaction active-interaction monitor channel])
  (get-closable [this monitor sender receiver]))

(defprotocol transportable
  (get-provider [this])
  (get-consumer [this])
  (get-chan [this])
  (get-monitor [this])
  (get-buffer [this]))

(defprotocol monitoring
  (get-monitor-id [this])
  ;(get-active-interaction [this])
  (apply-receive! [this target-interaction pre-swap-interaction sender receivers message])
  (apply-send! [this target-interaction pre-swap-interaction sender receivers message])
  (valid-send? [this sender receivers message])
  (valid-receive? [this sender receivers message])
  (valid-close? [this sender receiver])
  (apply-close! [this target-interaction pre-swap-interaction channel])
  (is-current-multicast? [this message])
  ;(get-rec [this name save-mapping])
  )

(load "monitor/core"
      "monitor/atomic"
      "monitor/branch"
      "monitor/close"
      "monitor/identifiable-recur"
      "monitor/parallel")

(defrecord interaction [id action sender receivers accepted-sends next]
  sendable
  (is-valid-sendable? [this monitor sender receivers message] (is-valid-sendable-atomic? this sender receivers message))
  (apply-sendable! [this pre-swap-interaction active-interaction monitor sender receivers message] (apply-sendable-atomic! this pre-swap-interaction active-interaction monitor sender))
  (get-sendable [this monitor sender receivers message] (get-sendable-atomic this sender receivers message))
  receivable
  (is-multicast? [this monitor message] (is-multicast-atomic? this message))
  (is-valid-receivable? [this monitor sender receivers message] (get-receivable-atomic this sender receivers message))
  (apply-receivable! [this pre-swap-interaction active-interaction monitor sender receivers message] (apply-receivable-atomic! this pre-swap-interaction active-interaction receivers))
  (get-receivable [this monitor sender receivers message] (get-receivable-atomic this sender receivers message))
  terminatable
  (is-valid-closable? [this monitor sender receiver] (is-valid-closable-atomic? this))
  (apply-closable! [this pre-swap-interaction active-interaction monitor channel] (apply-closable-atomic! this))
  (get-closable [this monitor sender receiver] (get-closable-atomic this)))

(defrecord closer [id sender receiver next]
  sendable
  (is-valid-sendable? [this monitor sender receivers message] (is-valid-sendable-closer? this monitor sender receivers message))
  (apply-sendable! [this pre-swap-interaction active-interaction monitor sender receivers message] (apply-sendable-closer! this pre-swap-interaction active-interaction monitor sender receivers message))
  (get-sendable [this monitor sender receivers message] (get-sendable-closer this monitor sender receivers message))
  receivable
  (is-multicast? [this monitor message] (is-multicast-closer?))
  (is-valid-receivable? [this monitor sender receivers message] (is-valid-receivable-closer? this monitor sender receivers message))
  (apply-receivable! [this pre-swap-interaction active-interaction monitor sender receivers message] (apply-receivable-closer! this pre-swap-interaction active-interaction monitor sender receivers message))
  (get-receivable [this monitor sender receivers message] (get-receivable-closer this monitor sender receivers message))
  terminatable
  (is-valid-closable? [this monitor sender receiver] (is-valid-closable-closer? this monitor sender receiver))
  (apply-closable! [this pre-swap-interaction active-interaction monitor channel] (apply-closable-closer! this pre-swap-interaction active-interaction monitor channel))
  (get-closable [this monitor sender receiver] (get-closable-closer this monitor sender receiver)))

(defrecord branch [id branches next]
  sendable
  (is-valid-sendable? [this monitor sender receivers message] (is-valid-sendable-branch? this monitor sender receivers message))
  (apply-sendable! [this pre-swap-interaction active-interaction monitor sender receivers message] (apply-sendable-branch! this pre-swap-interaction active-interaction monitor sender receivers message))
  (get-sendable [this monitor sender receivers message] (get-sendable-branch this monitor sender receivers message))
  receivable
  (is-multicast? [this monitor message] (is-multicast-branch? this monitor message))
  (is-valid-receivable? [this monitor sender receivers message] (is-valid-receivable-branch? this monitor sender receivers message))
  (apply-receivable! [this pre-swap-interaction active-interaction monitor sender receivers message] (apply-receivable-branch! this pre-swap-interaction active-interaction monitor sender receivers message))
  (get-receivable [this monitor sender receivers message] (get-receivable-branch this monitor sender receivers message))
  terminatable
  (is-valid-closable? [this monitor sender receiver] (is-valid-closable-branch? this monitor sender receiver))
  (apply-closable! [this pre-swap-interaction active-interaction monitor channel] (apply-closable-branch! this pre-swap-interaction active-interaction monitor channel))
  (get-closable [this monitor sender receiver] (get-closable-branch this monitor sender receiver)))

(defrecord lateral [id parallels next]
  sendable
  (is-valid-sendable? [this monitor sender receivers message] (is-valid-sendable-parallel? this monitor sender receivers message))
  (apply-sendable! [this pre-swap-interaction active-interaction monitor sender receivers message] (apply-sendable-parallel! this pre-swap-interaction active-interaction monitor sender receivers message))
  (get-sendable [this monitor sender receivers message] (get-sendable-parallel this monitor sender receivers message))
  receivable
  (is-multicast? [this monitor message] (is-multicast-parallel? this monitor message))
  (is-valid-receivable? [this monitor sender receivers message] (is-valid-receivable-parallel? this monitor sender receivers message))
  (apply-receivable! [this pre-swap-interaction active-interaction monitor sender receivers message] (apply-receivable-parallel! this pre-swap-interaction active-interaction monitor sender receivers message))
  (get-receivable [this monitor sender receivers message] (get-receivable-parallel this monitor sender receivers message))
  terminatable
  (is-valid-closable? [this monitor sender receiver] (is-valid-closable-parallel? this monitor sender receiver))
  (apply-closable! [this pre-swap-interaction active-interaction monitor channel] (apply-closable-parallel! this pre-swap-interaction active-interaction monitor channel))
  (get-closable [this monitor sender receiver] (get-closable-parallel this monitor sender receiver)))

(defrecord recursion [id name recursion next])

(defrecord recur-identifier [id name option next]
  sendable
  (is-valid-sendable? [this monitor sender receivers message] (is-valid-sendable-recur-identifier? this monitor sender receivers message))
  (apply-sendable! [this pre-swap-interaction active-interaction monitor sender receivers message] (apply-sendable-recur-identifier! this pre-swap-interaction active-interaction monitor sender receivers message))
  (get-sendable [this monitor sender receivers message] (get-sendable-recur-identifier this monitor sender receivers message))
  receivable
  (is-multicast? [this monitor message] (is-multicast-recur-identifier? this monitor message))
  (is-valid-receivable? [this monitor sender receivers message] (is-valid-receivable-recur-identifier? this monitor sender receivers message))
  (apply-receivable! [this pre-swap-interaction active-interaction monitor sender receivers message] (apply-receivable-recur-identifier! this pre-swap-interaction active-interaction monitor sender receivers message))
  (get-receivable [this monitor sender receivers message] (get-receivable-recur-identifier this monitor sender receivers message))
  terminatable
  (is-valid-closable? [this monitor sender receiver] (is-valid-closable-recur-identifier? this monitor sender receiver))
  (apply-closable! [this pre-swap-interaction active-interaction monitor channel] (apply-closable-recur-identifier! this pre-swap-interaction active-interaction monitor channel))
  (get-closable [this monitor sender receiver] (get-closable-recur-identifier this monitor sender receiver)))
