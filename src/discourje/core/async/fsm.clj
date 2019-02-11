(in-ns 'discourje.core.async.async)

(defprotocol edge
  (get-source-state [this])
  (get-sink-state [this])
  (get-action-label [this]))

(defprotocol stateful
  (get-id [this]))

(defrecord transition [source sink label]
  edge
  (get-source-state [this] source)
  (get-sink-state [this] sink)
  (get-action-label [this] label))

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
  (get-all-transitions [this])
  (get-input-transitions-for-node [this node])
  (get-output-transitions-for-node [this node])
  (get-transitions-by-label [this label])
  (get-nodes-by-label [this label]))

(defn- get-transitions-for-node
  "Get transitions for node"
  [node key transitions]
  (filter (fn [trans]
            (= (key trans) node))
          transitions))

(defn- get-transitions-with-label
  "Get all transitions with label"
  [label transitions]
  (filter (fn [trans] (= (get-action-label trans) label)) transitions))

(defn- get-nodes-with-label [label transitions]
  (let [labelled-transitions (get-transitions-with-label label transitions)
        nodes []]
    (conj nodes (flatten (for [trans labelled-transitions] [(get-source-state trans) (get-sink-state trans)])))))

(defrecord fsm [role start-node nodes transitions]
  finite-state-machine
  (get-role [this] role)
  (get-start-node [this] start-node)
  (get-nodes [this] nodes)
  (get-all-transitions [this] transitions)
  (get-input-transitions-for-node [this node] (get-transitions-for-node (get-id node) :sink transitions))
  (get-output-transitions-for-node [this node] (get-transitions-for-node (get-id node) :source transitions))
  (get-transitions-by-label [this label] (get-transitions-with-label label transitions))
  (get-nodes-by-label [this label] (get-nodes-with-label label transitions)))

(defrecord node [id]
  stateful
  (get-id [this] id))

(defn interactions-to-transitions [interactions]
  (let [result []
        query (fn [result]
                (let [result2 (flatten (vec (conj result [])))]
                  (conj result2
                        (flatten
                          (for [element interactions]
                            (cond
                              ;(instance? recursion element)
                              ;(flatten (vec (conj result2 (findAllParticipants (:protocol element) result2))))
                              ;(instance? choice element)
                              ;(let [trueResult (findAllParticipants (:trueBranch element) result2)
                              ;      falseResult (findAllParticipants (:falseBranch element) result2)]
                              ;  (if (not (nil? trueResult))
                              ;    (flatten (vec (conj result2 trueResult)))
                              ;    (flatten (vec (conj result2 falseResult)))))
                              (satisfies? discourje.core.async.async/interactable element)
                              (conj result2 (->transition (:sender element) (:receiver element) (:action element)))))))))
        x (query result)]
    (vec (first x))))