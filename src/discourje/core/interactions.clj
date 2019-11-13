;interactions.clj
(in-ns 'discourje.core.async)

(defprotocol sendable
  (is-valid-sendable? [this monitor sender receivers message])
  (apply-sendable! [this pre-swap-interaction active-interaction monitor sender receivers message])
  (get-sendable [this monitor sender receivers message]))

(defprotocol receivable
   (is-multicast?[this monitor message])
  ;(remove-receiver![this current-interaction receiver])
  (is-valid-receivable? [this monitor sender receivers message])
  (apply-receivable! [this pre-swap-interaction active-interaction monitor sender receivers message])
  (get-receivable [this monitor sender receivers message]))

(defprotocol terminatable
  (is-valid-closable? [this monitor sender receiver])
  (apply-closable! [this pre-swap-interaction active-interaction monitor channel])
  (get-closable [this monitor sender receiver]))

(defprotocol closable
  (get-from [this])
  (get-to [this]))

(defprotocol linkable
  (get-id [this])
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

(defprotocol swappable
  (get-pre-swap [this])
  (get-valid [this])
  (is-valid-for-swap? [this]))

(defrecord swappable-interaction [pre-swap valid]
  swappable
  (get-pre-swap [this] pre-swap)
  (get-valid [this] valid)
  (is-valid-for-swap? [this] (some? valid)))

(defprotocol branchable
  (get-branches [this]))

(defprotocol namable
  (get-name [this]))

(defprotocol recursable
  (get-recursion [this]))

(defprotocol identifiable-recur
  (get-option [this]))

(load "constructs/core"
      "constructs/atomic"
      "constructs/branch"
      "constructs/close"
      "constructs/identifiable-recur"
      "constructs/parallel")

(defrecord interaction [id action sender receivers accepted-sends next]
  interactable
  (get-action [this] action)
  (get-sender [this] sender)
  (get-receivers [this] receivers)
  (get-accepted-sends [this] accepted-sends)
  linkable
  (get-id [this] id)
  (get-next [this] next)
  stringify
  (to-string [this] (format "Interaction - Action: %s, Sender: %s, Receivers: %s with accepted sends %s" action sender receivers accepted-sends))
  sendable
  (is-valid-sendable? [this monitor sender receivers message] (is-valid-sendable-atomic? this sender receivers message))
  (apply-sendable! [this pre-swap-interaction active-interaction monitor sender receivers message] (apply-sendable-atomic! this pre-swap-interaction active-interaction sender))
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
  closable
  (get-from [this] sender)
  (get-to [this] receiver)
  linkable
  (get-id [this] id)
  (get-next [this] next)
  stringify
  (to-string [this] (format "Closer from Sender: %s to Receiver: %s" sender receiver))
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
  branchable
  (get-branches [this] branches)
  linkable
  (get-id [this] id)
  (get-next [this] next)
  stringify
  (to-string [this] (format "Branching with branches - %s" (apply str (for [b branches] (format "[ %s ]" (to-string b))))))
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
  parallelizable
  (get-parallel [this] parallels)
  linkable
  (get-id [this] id)
  (get-next [this] next)
  stringify
  (to-string [this] (format "Parallel with parallels - %s" (apply str (for [p parallels] (format "[ %s ]" (to-string p))))))
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

(defrecord recursion [id name recursion next]
  namable
  (get-name [this] name)
  recursable
  (get-recursion [this] recursion)
  linkable
  (get-id [this] id)
  (get-next [this] next)
  stringify
  (to-string [this] (format "Recursion name: %s, with recursion- %s" name (to-string recursion))))

(defrecord recur-identifier [id name option next]
  namable
  (get-name [this] name)
  identifiable-recur
  (get-option [this] option)
  linkable
  (get-id [this] id)
  (get-next [this] next)
  stringify
  (to-string [this] (format "Recur-identifier - name: %s, option: %s" name option))
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
                (conj result2 {:sender (get-sender element) :receivers (get-receivers element)})
                (satisfies? discourje.core.async/closable element)
                result2
                (satisfies? discourje.core.async/identifiable-recur element)
                result2
                :else
                (log-error :invalid-communication-type "Cannot find roles pairs for type:" element)))))))

(defn get-distinct-role-pairs
  "Get minimum amount of distinct sender and receivers pairs needed to implement the given protocol"
  [interactions]
  (vec (distinct (filter some? (flatten (find-all-role-pairs interactions []))))))