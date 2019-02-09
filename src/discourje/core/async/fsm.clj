(in-ns 'discourje.core.async.async)

(defprotocol edge
  (get-source-state [this])
  (get-sink-state [this])
  (get-action-label [this]))

(defprotocol stateful
  (get-id [this])
  (get-input-transitions [this])
  (get-output-transitions [this]))

(defrecord transition [source sink label]
  edge
  (get-source-state [this] source)
  (get-sink-state [this] sink)
  (get-action-label [this] label))

(defn- get-transitions
  "Get a transition"
  [id key transitions]
  (filter (fn [trans]
            (= (key trans) id))
          transitions))

(defn get-transitions-by-source
  "Get transitions by source"
  [id transitions]
  (get-transitions id :source transitions))

(defn get-transitions-by-sink
  "Get transitions by sink"
  [id transitions]
  (get-transitions id :sink transitions))

(defprotocol monitoring
  (get-active-states [this])
  (get-active-state [this role])
  (get-input-enabled-transitions [this])
  (get-output-enabled-transitions [this])
  (apply-action [this label]))


(defrecord monitor [fsms]
  monitoring
  (get-active-states [this])
  (get-active-state [this role])
  (get-input-enabled-transitions [this])
  (get-output-enabled-transitions [this])
  (apply-action [this label]))

(defrecord fsm [role start-node nodes transitions])

(defrecord node [id transitions active]
  stateful
  (get-id [this] id)
  (get-input-transitions [this] (get-transitions-by-sink id transitions))
  (get-output-transitions [this] (get-transitions-by-source id transitions)))

(defn generate-io-fsms
  "Convert a protocol of interactions to IO enabled finite-state-machines local to each role."
  [protocol]
  (let [roles (get-distinct-roles (get-interactions protocol))]))

