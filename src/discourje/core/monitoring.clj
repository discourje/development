;monitoring.clj
(in-ns 'discourje.core.async)

(defprotocol monitoring
  (get-monitor-id [this])
  (get-active-interaction [this])
  (apply-receive! [this sender receivers label])
  (apply-send! [this sender receivers label])
  (valid-send? [this sender receivers label])
  (valid-receive? [this sender receivers label])
  (valid-close? [this sender receiver])
  (apply-close! [this channel])
  (is-current-multicast? [this label])
  (register-rec! [this rec])
  (get-rec [this name]))

;load helper namespace files!
(load "validation/closevalidation"
      "validation/receivevalidation"
      "validation/sendvalidation")

(declare contains-value? is-valid-interaction?)

(defn- interaction-to-string
  "Stringify an interaction, returns empty string if the given interaction is nil"
  [interaction]
  (if (nil? interaction) "" (to-string interaction)))

(defn- is-valid-interaction?
  "Is the given interaction valid compared to the active-interaction of the monitor"
  [sender receivers label active-interaction]
  (and
    (= sender (:sender active-interaction))
    (and (if (instance? Seqable label)
           (or (contains-value? (:action active-interaction) label) (= label (:action active-interaction)))
           (or (nil? label) (= label (:action active-interaction)) (contains-value? label (:action active-interaction)))))
    (and (if (instance? Seqable (:receivers active-interaction))
           (or (contains-value? receivers (:receivers active-interaction)) (= receivers (:receivers active-interaction)))
           (or (= receivers (:receivers active-interaction)) (contains-value? (:receivers active-interaction) receivers))))))

(defn add-rec-to-table
  "Add a recursion to rec-table for continues to query"
  [rec-set rec]
  (when (nil? ((get-name rec) @rec-set))
    (swap! rec-set assoc (get-name rec) rec)))

(defn- contains-value?
  "Does the vector contain a value?"
  [element coll]
  (when (instance? Seqable coll)
    (boolean (some #(= element %) coll))))

(defn equal-monitors?
  "Check if all channels have the same monitor"
  [channels]
  (= 1 (count (distinct (for [c channels] (get-monitor-id (get-monitor c)))))))

(defn- is-active-interaction-multicast? [monitor active-interaction label]
  (cond
    (satisfies? interactable active-interaction)
    (and (or (nil? label) (= (get-action active-interaction) label) (contains-value? (get-action active-interaction) label)) (instance? Seqable (get-receivers active-interaction)))
    (satisfies? branchable active-interaction)
    (> (count (filter true? (flatten (for [b (:branches active-interaction)] (is-active-interaction-multicast? monitor b label))))) 0)
    (satisfies? parallelizable active-interaction)
    (> (count (filter true? (flatten (for [p (get-parallel active-interaction)] (is-active-interaction-multicast? monitor p label))))) 0)
    (satisfies? recursable active-interaction)
    (is-active-interaction-multicast? monitor (get-recursion active-interaction) label)
    (satisfies? identifiable-recur active-interaction)
    (is-active-interaction-multicast? monitor (get-rec monitor (get-name (get-next active-interaction))) label)
    :else
    (do (log-error :unsupported-operation (format "Unsupported communication type: Communication invalid, type: %s" (type active-interaction)))
        false)))

(defn force-monitor-reset! "Force the monitor to go back to the first interaction." [monitor]
  (reset! (:active-interaction monitor) (:interactions monitor)))

(defrecord monitor [id interactions active-interaction recursion-set]
  monitoring
  (get-monitor-id [this] id)
  (get-active-interaction [this] @active-interaction)
  (apply-send! [this sender receivers label] (apply-send-to-mon this sender receivers label active-interaction @active-interaction))
  (apply-receive! [this sender receivers label] (apply-receive-to-mon this sender receivers label active-interaction @active-interaction))
  (valid-send? [this sender receivers label] (is-valid-send-communication? this sender receivers label @active-interaction))
  (valid-receive? [this sender receivers label] (is-valid-communication? this sender receivers label @active-interaction))
  (is-current-multicast? [this label] (is-active-interaction-multicast? this @active-interaction label))
  (register-rec! [this rec] (add-rec-to-table recursion-set rec))
  (get-rec [this name] (name @recursion-set))
  (valid-close? [this sender receiver] (is-valid-communication? this sender receiver nil @active-interaction))
  (apply-close! [this channel] (apply-close-to-mon this channel active-interaction @active-interaction)))