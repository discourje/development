;close construct
(in-ns 'discourje.core.async)

;;---------------------------------Sendable implementation-------------------------------------------------
(defn- is-valid-sendable-closer? [active-interaction monitor sender receivers message]
  nil)

(defn- get-sendable-closer
  "Check the atomic interaction"
  [active-interaction monitor sender receivers message]
  nil)

(defn- apply-sendable-closer! [active-interaction monitor sender receivers message pre-swap-interaction target-interaction]
  (log-error :unsupported-operation (format "Unsupported type of interaction to apply %s!" (type target-interaction))))

;;--------------------------------Receivable implementation------------------------------------------------
(defn- is-valid-receivable-closer? [active-interaction monitor sender receivers message]
  nil)

(defn- get-receivable-closer
  "Check the atomic interaction"
  [active-interaction monitor sender receivers message]
  nil)

(defn- apply-receivable-closer! [active-interaction monitor sender receivers message pre-swap-interaction target-interaction]
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

(defn- apply-closable-closer! [active-interaction monitor channel pre-swap-interaction target-interaction]
  (apply-receivable-atomic! active-interaction pre-swap-interaction target-interaction nil))