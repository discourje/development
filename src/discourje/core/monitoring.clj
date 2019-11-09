;monitoring.clj
(in-ns 'discourje.core.async)

(defprotocol monitoring
  (get-monitor-id [this])
  (get-active-interaction [this])
  (apply-receive! [this target-interaction pre-swap-interaction sender receivers message])
  (apply-send! [this  target-interaction pre-swap-interaction sender receivers message])
  (valid-send? [this sender receivers message])
  (valid-receive? [this sender receivers message])
  (valid-close? [this sender receiver])
  (apply-close! [this target-interaction pre-swap-interaction channel])
  (is-current-multicast? [this message])
  (get-rec [this name]))

(defn- interaction-to-string
  "Stringify an interaction, returns empty string if the given interaction is nil"
  [interaction]
  (if (satisfies? stringify interaction) (to-string interaction) interaction))

;load helper namespace files!
;(load "validation/closevalidation"
;      "validation/receivevalidation"
;      "validation/sendvalidation")

(declare contains-value? is-valid-interaction?)

(defn is-predicate-valid?
  "Is the predicate in the monitor valid compared to the message or label (when given)"
  [message active-interaction]
  ((get-action active-interaction) message))

(defn- is-valid-interaction?
  "Is the given interaction valid compared to the active-interaction of the monitor"
  [sender receivers message active-interaction]
  (and
    (= sender (:sender active-interaction))
    (is-predicate-valid? message active-interaction)
    (and (if (instance? Seqable (:receivers active-interaction))
           (or (contains-value? receivers (:receivers active-interaction)) (= receivers (:receivers active-interaction)))
           (or (= receivers (:receivers active-interaction)) (contains-value? (:receivers active-interaction) receivers))))))

(defn- contains-value?
  "Does the vector contain a value?"
  [element coll]
  (when (instance? Seqable coll)
    (boolean (some #(= element %) coll))))

(defn equal-monitors?
  "Check if all channels have the same monitor"
  [channels]
  (= 1 (count (distinct (for [c channels] (get-monitor-id (get-monitor c)))))))

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

(defn force-monitor-reset! "Force the monitor to go back to the first interaction." [monitor interactions]
  (reset! (:active-interaction monitor) interactions))

(defrecord monitor [id active-interaction recursion-set]
  monitoring
  (get-monitor-id [this] id)
  (get-active-interaction [this] @active-interaction)
  (apply-send! [this target-interaction pre-swap-interaction sender receivers message] (apply-sendable! target-interaction pre-swap-interaction active-interaction this sender receivers message))
  (apply-receive! [this target-interaction pre-swap-interaction sender receivers message] (apply-receivable! target-interaction pre-swap-interaction active-interaction this sender receivers message))
  (valid-send? [this sender receivers message] (let [pre-swap @active-interaction]
                                                 (->swappable-interaction pre-swap (is-valid-sendable? pre-swap this sender receivers message))))
  (valid-receive? [this sender receivers message] (let [pre-swap @active-interaction]
                                                    (->swappable-interaction pre-swap (is-valid-receivable? pre-swap this sender receivers message))))
  (is-current-multicast? [this message] (is-active-interaction-multicast? this @active-interaction message))
  (get-rec [this name] (name @recursion-set))
  (valid-close? [this sender receiver] (let [pre-swap @active-interaction]
                                         (->swappable-interaction pre-swap (is-valid-closable? pre-swap this sender receiver))))
  (apply-close! [this target-interaction pre-swap-interaction channel] (apply-closable! target-interaction pre-swap-interaction active-interaction this channel)))