;close construct
(in-ns 'discourje.core.async.impl.monitor)

;;---------------------------------Sendable implementation-------------------------------------------------
(defn- is-valid-sendable-closer? [active-interaction monitor sender receivers message]
  nil)

(defn- get-sendable-closer
  "Check the atomic interaction"
  [active-interaction monitor sender receivers message]
  nil)

(defn- apply-sendable-closer! [target-interaction  pre-swap-interaction active-interaction monitor sender receivers message]
  (log-error :unsupported-operation (format "Unsupported type of interaction to apply %s!" (type target-interaction))))

;;--------------------------------Receivable implementation------------------------------------------------
(defn is-multicast-closer? []
  false)

(defn- is-valid-receivable-closer? [active-interaction monitor sender receivers message]
  nil)

(defn- get-receivable-closer
  "Check the atomic interaction"
  [active-interaction monitor sender receivers message]
  nil)

(defn- apply-receivable-closer! [target-interaction pre-swap-interaction active-interaction monitor sender receivers message]
  (log-error :unsupported-operation (format "Unsupported type of interaction to apply %s!" (type target-interaction))))
;;---------------------------------Closable implementation-------------------------------------------------
(defn- is-valid-close?
  "Is the active interaction a valid close?"
  [sender receivers active-interaction]
  (and
    (= sender (get-from active-interaction))
    (= receivers (get-to active-interaction))))

(defn- is-valid-closable-closer? [active-interaction monitor sender receiver]
  (when (is-valid-close? sender receiver active-interaction)
    active-interaction))

(defn- get-closable-closer
  "Check the atomic interaction"
  [active-interaction monitor sender receiver]
  (when (is-valid-close? sender receiver active-interaction)
    active-interaction))

(defn- apply-closable-closer! [target-interaction pre-swap-interaction active-interaction monitor channel]
  (apply-receivable-atomic! target-interaction pre-swap-interaction active-interaction nil))