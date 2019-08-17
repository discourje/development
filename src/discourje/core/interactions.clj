;interactions.clj
(in-ns 'discourje.core.async)

(defprotocol idable
  (get-id [this]))

(defprotocol linkable
  (get-next [this]))

(defprotocol interactable
  (get-action [this])
  (get-sender [this])
  (get-receivers [this])
  (get-accepted-sends [this]))

(defprotocol parallelizable
  (get-parallel [this]))

(defprotocol stringify
  (to-string [this]))

(defrecord interaction [id action sender receivers accepted-sends next]
  idable
  (get-id [this] id)
  interactable
  (get-action [this] action)
  (get-sender [this] sender)
  (get-receivers [this] receivers)
  (get-accepted-sends [this] accepted-sends)
  linkable
  (get-next [this] next)
  stringify
  (to-string [this] (format "Interaction - Action: %s, Sender: %s, Receivers: %s" action sender receivers)))

(defprotocol branchable
  (get-branches [this]))

(defrecord branch [id branches next]
  idable
  (get-id [this] id)
  branchable
  (get-branches [this] branches)
  linkable
  (get-next [this] next)
  stringify
  (to-string [this] (format "Branching with branches - %s" (apply str (for [b branches] (format "[ %s ]" (to-string b)))))))

(defrecord parallel [id parallels next]
  idable
  (get-id [this] id)
  parallelizable
  (get-parallel [this] parallels)
  linkable
  (get-next [this] next)
  stringify
  (to-string [this] (format "Parallel with parallels - %s" (apply str (for [p parallels] (format "[ %s ]" (to-string p)))))))

(defprotocol namable
  (get-name [this]))

(defprotocol recursable
  (get-recursion [this]))

(defrecord recursion [id name recursion next]
  idable
  (get-id [this] id)
  namable
  (get-name [this] name)
  recursable
  (get-recursion [this] recursion)
  linkable
  (get-next [this] next)
  stringify
  (to-string [this] (format "Recursion name: %s, with recursion- %s" name (to-string recursion))))

(defprotocol identifiable-recur
  (get-option [this]))

(defrecord recur-identifier [id name option next]
  idable
  (get-id [this] id)
  namable
  (get-name [this] name)
  identifiable-recur
  (get-option [this] option)
  linkable
  (get-next [this] next)
  stringify
  (to-string [this] (format "Recur-identifier - name: %s, option: %s" name option)))

(defn- find-all-roles
  "List all sender and receivers in the protocol"
  [protocol result]
  (let [result2 (flatten (vec (conj result [])))]
    (conj result2
          (flatten
            (for [element protocol]
              (cond
                (satisfies? discourje.core.async/recursable element)
                (conj result2 (flatten (find-all-roles (:recursion element) result2)))
                (satisfies? discourje.core.async/branchable element)
                (let [branched-interactions (for [branch (get-branches element)] (find-all-roles branch result2))]
                  (conj result2 (flatten branched-interactions)))
                (satisfies? discourje.core.async/parallelizable element)
                (let [parallel-interactions (for [p (get-parallel element)] (find-all-roles p result2))]
                  (conj result2 (flatten parallel-interactions)))
                (satisfies? discourje.core.async/interactable element)
                (do
                  (if (instance? Seqable (get-receivers element))
                    (conj result2 (flatten (get-receivers element)) (get-sender element))
                    (conj result2 (get-receivers element) (get-sender element))))))))))

(defn- find-all-role-pairs
  "List all sender and receivers in the protocol"
  [protocol result]
  (let [result2 (conj result [])]
    (conj result2
          (flatten
            (for [element protocol]
              (cond
                (satisfies? discourje.core.async/recursable element)
                (conj result2 (flatten (find-all-role-pairs (get-recursion element) result2)))
                (satisfies? discourje.core.async/branchable element)
                (let [branched-interactions (for [branch (get-branches element)] (find-all-role-pairs branch result2))]
                  (conj result2 (flatten branched-interactions)))
                (satisfies? discourje.core.async/parallelizable element)
                (let [parallel-interactions (for [p (get-parallel element)] (find-all-role-pairs p result2))]
                  (conj result2 (flatten parallel-interactions)))
                (satisfies? discourje.core.async/interactable element)
                (conj result2 {:sender (get-sender element) :receivers (get-receivers element)})))))))

(defn get-distinct-roles
  "Get all distinct senders and receivers in the protocol"
  [interactions]
  (let [x (find-all-roles interactions [])]
    (vec (filter some? (distinct (flatten (first x)))))))

(defn get-distinct-role-pairs
  "Get minimum amount of distinct sender and receivers pairs needed to implement the given protocol"
  [interactions]
  (vec (distinct (filter some? (flatten (find-all-role-pairs interactions []))))))

(defn get-interactions-by-role [role protocol]
  (vec (some? (filter
                (fn [interaction]
                  (when (or
                          (= (get-sender interaction) role)
                          (= (get-receivers interaction) role))
                    interaction)) protocol))))