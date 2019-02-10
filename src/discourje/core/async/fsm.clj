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

(defprotocol finite-state-machine
  (get-role [this])
  (get-start-node [this])
  (get-nodes [this])
  (get-all-transitions[this])
  (get-input-transitions-for-node [this node])
  (get-output-transitions-for-node [this node])
  (get-transitions-by-label [this label])
  (get-nodes-by-label [this label])
  (set-active-node [this node]))

(defn- get-transitions-for-node
  "Get transitions for node"
  [node key transitions]
  (filter (fn [trans]
            (= (key trans) node))
          transitions))

(defn- get-transitions-with-label
  "Get all transitions with label"
  [label transitions]
  (filter (fn [trans] (= (get-label trans) label)) transitions))

(defn- get-nodes-with-label [label transitions]
  (let [labelled-transitions (get-transitions-with-label label transitions)
        nodes []]
    (conj nodes (flatten (for [trans transitions] [(get-source-state trans) (get-sink-state trans)])))))

(defrecord fsm [role start-node nodes transitions]
  finite-state-machine
  (get-role [this] role)
  (get-start-node [this] start-node)
  (get-nodes [this] nodes)
  (get-all-transitions[this] transitions)
  (get-input-transitions-for-node [this node] (get-transitions-for-node (get-id node) :sink transitions))
  (get-output-transitions-for-node [this node](get-transitions-for-node (get-id node) :source transitions))
  (get-transitions-by-label [this label] (get-transitions-with-label label transitions))
  (get-nodes-by-label [this label] (get-nodes-with-label label transitions))
  (set-active-node [this node] ()) ;todo continue here!
  )

(defrecord node [id transitions active]
  stateful
  (get-id [this] id)
  (get-input-transitions [this] (get-transitions-by-sink id transitions))
  (get-output-transitions [this] (get-transitions-by-source id transitions)))

(defn generate-io-fsms
  "Convert a protocol of interactions to IO enabled finite-state-machines local to each role."
  [protocol]
  (let [roles (get-distinct-roles (get-interactions protocol))]))

