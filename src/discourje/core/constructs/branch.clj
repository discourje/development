;branch construct
(in-ns 'discourje.core.async)

;;---------------------------------Sendable implementation-------------------------------------------------
(defn- is-valid-sendable-branch? [active-interaction monitor sender receivers message]
  (first (filter #(is-valid-sendable? % monitor sender receivers message) (get-branches active-interaction))))

(defn- get-sendable-branch
  "Check the atomic interaction"
  [active-interaction monitor sender receivers message]
  (first (filter #(get-sendable % monitor sender receivers message) (get-branches active-interaction))))

(defn- apply-sendable-branch! [active-interaction monitor sender receivers message pre-swap-interaction target-interaction]
  (apply-sendable-branch! active-interaction monitor sender receivers message pre-swap-interaction
                          (get-sendable-branch target-interaction monitor sender receivers message)))

;;--------------------------------Receivable implementation------------------------------------------------
(defn- is-valid-receivable-branch? [active-interaction monitor sender receivers message]
  (first (filter #(is-valid-receivable? % monitor sender receivers message) (get-branches active-interaction))))

(defn- get-receivable-branch
  "Check the atomic interaction"
  [active-interaction monitor sender receivers message]
  (first (filter #(get-receivable % monitor sender receivers message) (get-branches active-interaction))))

(defn- apply-receivable-branch! [active-interaction monitor sender receivers message pre-swap-interaction target-interaction]
  (apply-receivable-branch! active-interaction monitor sender receivers message pre-swap-interaction
                          (get-receivable-branch target-interaction monitor sender receivers message)))
;;---------------------------------Closable implementation-------------------------------------------------

(defn- is-valid-closable-branch? [active-interaction monitor sender receiver]
  (first (filter #(is-valid-closable-branch? % monitor sender receiver) (get-branches active-interaction))))

(defn- get-closable-branch
  "Check the atomic interaction"
  [active-interaction monitor sender receiver]
  (first (filter #(get-closable % monitor sender receiver) (get-branches active-interaction))))

(defn- apply-closable-branch! [active-interaction monitor channel pre-swap-interaction target-interaction]
  (apply-closable-branch! active-interaction monitor channel pre-swap-interaction
                            (get-closable-branch target-interaction monitor (get-provider channel) (get-consumer channel))))