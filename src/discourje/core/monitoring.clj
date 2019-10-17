;monitoring.clj
(in-ns 'discourje.core.async)

(defprotocol monitoring
  (get-monitor-id [this])
  (get-active-interaction [this])
  (apply-receive! [this sender receivers message pre-swap-interaction target-interaction])
  (apply-send! [this sender receivers message pre-swap-interaction target-interaction])
  (valid-send? [this sender receivers message])
  (valid-receive? [this sender receivers message])
  (valid-close? [this sender receiver])
  (apply-close! [this channel pre-swap-interaction target-interaction])
  (is-current-multicast? [this message label])
  (get-rec [this name]))

(defn- interaction-to-string
  "Stringify an interaction, returns empty string if the given interaction is nil"
  [interaction]
  (if (satisfies? stringify interaction) (to-string interaction) interaction))

;load helper namespace files!
(load "validation/closevalidation"
      "validation/receivevalidation"
      "validation/sendvalidation")

(declare contains-value? is-valid-interaction?)

(defn- is-valid-interaction?
  "Is the given interaction valid compared to the active-interaction of the monitor"
  [sender receivers message active-interaction]
  (and
    (= sender (:sender active-interaction))
    (and (if (callable? (get-action active-interaction))
           ((get-action active-interaction) message)
           (or (nil? (get-action active-interaction)) (= (type message) (get-action active-interaction)))))
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

(defn- is-active-interaction-multicast? [monitor active-interaction message label]
  (cond
    (satisfies? interactable active-interaction)
    (and (if (callable? (get-action active-interaction))
           ((get-action active-interaction) (if (nil? label) message label))
           (or (nil? (get-action active-interaction)) (= (type message) (get-action active-interaction))))
         (instance? Seqable (get-receivers active-interaction)))
    (satisfies? branchable active-interaction)
    (first (filter #(is-active-interaction-multicast? monitor % message label) (get-branches active-interaction)))
    (satisfies? parallelizable active-interaction)
    (first (filter #(is-active-interaction-multicast? monitor % message label) (get-parallel active-interaction)))
    (satisfies? identifiable-recur active-interaction)
    (is-active-interaction-multicast? monitor (get-rec monitor (get-name (get-next active-interaction))) message label)
    :else
    (do (log-error :unsupported-operation (format "Unsupported communication type: Communication invalid, type: %s" (type active-interaction)))
        false)))

(defn force-monitor-reset! "Force the monitor to go back to the first interaction." [monitor interactions]
  (reset! (:active-interaction monitor) interactions))

(defrecord monitor [id active-interaction recursion-set]
  monitoring
  (get-monitor-id [this] id)
  (get-active-interaction [this] @active-interaction)
  (apply-send! [this sender receivers message pre-swap-interaction target-interaction] (apply-send-to-mon this sender receivers message active-interaction pre-swap-interaction target-interaction))
  (apply-receive! [this sender receivers message pre-swap-interaction target-interaction] (apply-receive-to-mon this sender receivers message active-interaction pre-swap-interaction target-interaction))
  (valid-send? [this sender receivers message] (let [pre-swap @active-interaction]
                                                 (->swappable-interaction pre-swap (is-valid-send-communication? this sender receivers message pre-swap))))
  (valid-receive? [this sender receivers message] (let [pre-swap @active-interaction]
                                                    (->swappable-interaction pre-swap (is-valid-communication? this sender receivers message pre-swap))))
  (is-current-multicast? [this message label] (is-active-interaction-multicast? this @active-interaction message label))
  (get-rec [this name] (name @recursion-set))
  (valid-close? [this sender receiver] (let [pre-swap @active-interaction]
                                         (->swappable-interaction pre-swap (is-valid-close-communication? this sender receiver pre-swap))))
  (apply-close! [this channel pre-swap-interaction target-interaction] (apply-close-to-mon this channel active-interaction pre-swap-interaction target-interaction)))