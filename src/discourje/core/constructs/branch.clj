;branch construct
(in-ns 'discourje.core.async)

;;---------------------------------Sendable implementation-------------------------------------------------
(defn- is-valid-sendable-branch? [active-interaction monitor sender receivers message]
  (first (filter #(is-valid-sendable? % monitor sender receivers message) (get-branches active-interaction))))

(defn- get-sendable-branch
  "Check the atomic interaction"
  [active-interaction monitor sender receivers message]
  (first (filter #(get-sendable % monitor sender receivers message) (get-branches active-interaction))))

(defn- apply-sendable-branch! [target-interaction pre-swap-interaction active-interaction monitor sender receivers message]
  (apply-sendable! (get-sendable-branch target-interaction monitor sender receivers message) pre-swap-interaction active-interaction monitor sender receivers message))

;;--------------------------------Receivable implementation------------------------------------------------
(defn is-multicast-branch? [active-interaction monitor message]
  (first (filter #(is-multicast? % monitor message) (get-branches active-interaction))))

(defn- is-valid-receivable-branch? [active-interaction monitor sender receivers message]
  (first (filter #(is-valid-receivable? % monitor sender receivers message) (get-branches active-interaction))))

(defn- get-receivable-branch
  "Check the atomic interaction"
  [active-interaction monitor sender receivers message]
  (first (filter #(get-receivable % monitor sender receivers message) (get-branches active-interaction))))

(defn- apply-receivable-branch! [target-interaction pre-swap-interaction active-interaction monitor sender receivers message]
  (apply-receivable! (get-receivable-branch target-interaction monitor sender receivers message) pre-swap-interaction active-interaction monitor sender receivers message))
;;---------------------------------Closable implementation-------------------------------------------------

(defn- is-valid-closable-branch? [active-interaction monitor sender receiver]
  (first (filter #(is-valid-closable? % monitor sender receiver) (get-branches active-interaction))))

(defn- get-closable-branch
  "Check the atomic interaction"
  [active-interaction monitor sender receiver]
  (first (filter #(get-closable % monitor sender receiver) (get-branches active-interaction))))

(defn- apply-closable-branch! [target-interaction pre-swap-interaction active-interaction monitor channel]
  (apply-closable! (get-closable-branch target-interaction monitor (get-provider channel) (get-consumer channel)) pre-swap-interaction active-interaction monitor channel))