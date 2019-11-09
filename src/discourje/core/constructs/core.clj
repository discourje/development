;core.clj
(in-ns 'discourje.core.async)

(defn is-predicate-valid?
  "Is the predicate in the monitor valid compared to the message or label (when given)"
  [message active-interaction]
  ((get-action active-interaction) message))

(defn- contains-value?
  "Does the vector contain a value?"
  [element coll]
  (when (instance? Seqable coll)
    (boolean (some #(= element %) coll))))

(defn- is-valid-interaction?
  "Is the given interaction valid compared to the active-interaction of the monitor"
  [sender receivers message active-interaction]
  (and
    (= sender (:sender active-interaction))
    (is-predicate-valid? message active-interaction)
    (and (if (instance? Seqable (:receivers active-interaction))
           (or (contains-value? receivers (:receivers active-interaction)) (= receivers (:receivers active-interaction)))
           (or (= receivers (:receivers active-interaction)) (contains-value? (:receivers active-interaction) receivers))))))

(defn- is-active-interaction-multicast? [monitor active-interaction message]
  (cond
    (satisfies? interactable active-interaction)
    (and (is-predicate-valid? message active-interaction)
         (instance? Seqable (get-receivers active-interaction)))
    (satisfies? branchable active-interaction)
    (first (filter #(is-active-interaction-multicast? monitor % message) (get-branches active-interaction)))
    (satisfies? parallelizable active-interaction)
    (first (filter #(is-active-interaction-multicast? monitor % message) (get-parallel active-interaction)))
    (satisfies? identifiable-recur active-interaction)
    (is-active-interaction-multicast? monitor (get-rec monitor (get-name (get-next active-interaction))) message)
    :else
    (do (log-error :unsupported-operation (format "Unsupported communication type: Communication invalid, type: %s" (type active-interaction)))
        false)))

