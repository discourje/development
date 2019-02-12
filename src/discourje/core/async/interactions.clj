(in-ns 'discourje.core.async.async)

(defprotocol interactable
  (get-action [this])
  (get-sender [this])
  (get-receivers [this]))

(defrecord interaction [action sender receivers]
  interactable
  (get-action [this] action)
  (get-sender [this] sender)
  (get-receivers [this] receivers))

(defn- find-all-roles
  "List all sender and receivers in the protocol"
  [protocol result]
  (let [result2 (flatten (vec (conj result [])))]
    (conj result2
          (flatten
            (for [element protocol]
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
                (do
                  (if (instance? Seqable (get-receivers element))
                    (conj result2 (flatten (get-receivers element)) (get-sender element))
                    (conj result2 (get-receivers element) (get-sender element))))))))))

(defn get-distinct-roles
  "Get all distinct senders and receivers in the protocol"
  [interactions]
  (let [x (find-all-roles interactions [])]
    (vec (filter some? (distinct (flatten (first x)))))))

(defn get-interactions-by-role [role protocol]
  (vec (some? (filter
                (fn [interaction]
                  (when (or
                          (= (get-sender interaction) role)
                          (= (get-receivers interaction) role))
                    interaction))
                protocol))))