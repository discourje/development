;parallel construct
(in-ns 'discourje.core.async)

;;---------------------------------Sendable implementation-------------------------------------------------
(defn- is-valid-sendable-parallel? [active-interaction monitor sender receivers message]
  (when-let [_ (first (filter
                        #(is-valid-sendable? % monitor sender receivers message) (get-parallel active-interaction)))]
    active-interaction))

(defn- get-sendable-branch
  "Check the atomic interaction"
  [active-interaction monitor sender receivers message]
  (when-let [_ (first (filter
                        #(is-valid-sendable? % monitor sender receivers message) (get-parallel active-interaction)))]
    active-interaction))

(defn- set-send-on-parallel
  "Assoc a sender to a nested parallel interaction"
  [sender receivers message target-interaction monitor]
  (let [pars (let [pars (get-parallel target-interaction)
                   is-found (atom false)]
               (for [p pars]
                 (cond
                   @is-found p
                   (is-valid-sendable? target-interaction monitor sender receivers message)
                   (let [valid (get-sendable target-interaction monitor sender receivers message)]
                     (assoc-sender valid sender is-found))
                   :else
                   p
                   )))]
    (let [duplicate-par (first (filter some? (filter (fn [p] (= (get-id p) (get-id target-interaction))) pars)))]
      (if (nil? duplicate-par)
        (assoc target-interaction :parallels pars)
        duplicate-par))))

(defn- apply-sendable-branch! [active-interaction monitor sender receivers message pre-swap-interaction target-interaction]
  (swap! active-interaction
         (fn [inter]
           (if (= (get-id inter) (get-id target-interaction))
             (set-send-on-parallel sender receivers message inter monitor)
             (set-send-on-parallel sender receivers message target-interaction monitor))))
  true)

;;--------------------------------Receivable implementation------------------------------------------------
(defn- is-valid-receivable-branch? [active-interaction monitor sender receivers message]
  (first (filter #(is-valid-receivable? % monitor sender receivers message) (get-branches active-interaction))))

(defn- get-receivable-branch
  "Check the atomic interaction"
  [active-interaction monitor sender receivers message]
  (first (filter #(get-receivable % monitor sender receivers message) (get-branches active-interaction))))

(defn- apply-receivable-branch! [active-interaction monitor sender receivers message pre-swap-interaction target-interaction]
  (apply-receivable! active-interaction monitor sender receivers message pre-swap-interaction
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