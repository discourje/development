;identifiable-recur construct
(in-ns 'discourje.core.async.impl.monitor)

;;---------------------------------Sendable implementation-------------------------------------------------
(defn- is-valid-sendable-recur-identifier? [active-interaction monitor sender receivers message]
  (if (satisfies? parallelizable active-interaction)
    (when (<= (count (get-parallel active-interaction)) 1)
      (is-valid-sendable? (get-rec monitor (get-name active-interaction) false) monitor sender receivers message))
    (is-valid-sendable? (get-rec monitor (get-name active-interaction) false) monitor sender receivers message)))

(defn- get-sendable-recur-identifier
  "Check the atomic interaction"
  [active-interaction monitor sender receivers message]
  (get-sendable (get-rec monitor (get-name active-interaction) false) monitor sender receivers message))

(defn- apply-sendable-recur-identifier! [target-interaction pre-swap-interaction active-interaction monitor sender receivers message]
  (apply-sendable! (get-rec monitor (get-name target-interaction) true) pre-swap-interaction active-interaction monitor sender receivers message))

;;--------------------------------Receivable implementation------------------------------------------------
(defn is-multicast-recur-identifier? [active-interaction monitor message]
  (is-multicast? (get-rec monitor (get-name active-interaction) false) monitor message))

(defn- is-valid-receivable-recur-identifier? [active-interaction monitor sender receivers message]
  (is-valid-receivable? monitor sender receivers message (get-rec monitor (get-name active-interaction) false)))

(defn- get-receivable-recur-identifier
  "Check the atomic interaction"
  [active-interaction monitor sender receivers message]
  (get-receivable active-interaction monitor sender receivers message))

(defn- apply-receivable-recur-identifier! [target-interaction pre-swap-interaction active-interaction monitor sender receivers message]
  (apply-receivable! (get-rec monitor (get-name target-interaction) true) pre-swap-interaction active-interaction monitor sender receivers message))
;;---------------------------------Closable implementation-------------------------------------------------

(defn- is-valid-closable-recur-identifier? [active-interaction monitor sender receiver]
  (if (satisfies? parallelizable active-interaction)
    (when (<= (count (get-parallel active-interaction)) 1)
      (is-valid-closable? (get-rec monitor (get-name active-interaction) false) monitor sender receiver))
    (is-valid-closable? (get-rec monitor (get-name active-interaction) false) monitor sender receiver)))

(defn- get-closable-recur-identifier
  "Check the atomic interaction"
  [active-interaction monitor sender receiver]
  nil)

(defn- apply-closable-recur-identifier! [target-interaction pre-swap-interaction active-interaction monitor channel]
  (apply-closable! (get-rec monitor (get-name target-interaction) true) pre-swap-interaction active-interaction monitor channel))