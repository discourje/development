;identifiable-recur construct
(in-ns 'discourje.core.async)

;;---------------------------------Sendable implementation-------------------------------------------------
(defn- is-valid-sendable-recur-identifier? [active-interaction monitor sender receivers message]
  (if (satisfies? parallelizable active-interaction)
    (when (<= (count (get-parallel active-interaction)) 1)
      (is-valid-sendable? (get-rec monitor (get-name active-interaction)) monitor sender receivers message))
    (is-valid-sendable? (get-rec monitor (get-name active-interaction)) monitor sender receivers message)))

(defn- get-sendable-recur-identifier
  "Check the atomic interaction"
  [active-interaction monitor sender receivers message]
  (get-sendable (get-rec monitor (get-name active-interaction)) monitor sender receivers message))

(defn- apply-sendable-recur-identifier! [active-interaction monitor sender receivers message pre-swap-interaction target-interaction]
  (apply-sendable! active-interaction monitor sender receivers message pre-swap-interaction (get-rec monitor (get-name target-interaction))))

;;--------------------------------Receivable implementation------------------------------------------------
(defn- is-valid-receivable-recur-identifier? [active-interaction monitor sender receivers message]
  (is-valid-receivable? monitor sender receivers message (get-rec monitor (get-name active-interaction))))

(defn- get-receivable-recur-identifier
  "Check the atomic interaction"
  [active-interaction monitor sender receivers message]
  (get-receivable active-interaction monitor sender receivers message))

(defn- apply-receivable-recur-identifier! [active-interaction monitor sender receivers message pre-swap-interaction target-interaction]
  (apply-receivable! active-interaction monitor sender receivers message pre-swap-interaction (get-rec monitor (get-name target-interaction))))
;;---------------------------------Closable implementation-------------------------------------------------

(defn- is-valid-closable-recur-identifier? [active-interaction monitor sender receiver]
  (if (satisfies? parallelizable active-interaction)
    (when (<= (count (get-parallel active-interaction)) 1)
      (is-valid-closable? (get-rec monitor (get-name active-interaction)) monitor sender receiver))
    (is-valid-closable? (get-rec monitor (get-name active-interaction)) monitor sender receiver)))

(defn- get-closable-recur-identifier
  "Check the atomic interaction"
  [active-interaction monitor sender receiver]
  nil)

(defn- apply-closable-recur-identifier! [active-interaction monitor channel pre-swap-interaction target-interaction]
  (apply-closable! active-interaction monitor channel pre-swap-interaction (get-rec monitor (get-name target-interaction))))