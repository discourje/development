;monitoring.clj
(in-ns 'discourje.core.async.async)

(defprotocol monitoring
  (get-monitor-id [this])
  (get-active-interaction [this])
  (apply-interaction [this label])
  (valid-interaction? [this sender receivers label]))

(defn- check-atomic-interaction
  "Check the atomic interaction"
  [label active-interaction]
  (= (get-action @active-interaction) label))

(defn- swap-next-interaction!
  "Get the next interaction"
  [interactions]
  (fn [active-interaction]
    (first (filter
            (fn [inter]
             (cond (instance? interaction active-interaction) (= (get-id inter) (get-next active-interaction))
                   :else (do (println "Not supported type!") false)))
           interactions))))

(defn- swap-active-interaction-by-atomic
  "Swap active interaction by atomic"
  [active-interaction interactions]
  (swap! active-interaction (swap-next-interaction! interactions)))

(defn- apply-interaction-to-mon
  "Apply new interaction"
  [label active-interaction interactions]
  (cond
    (and (instance? interaction @active-interaction) (check-atomic-interaction label active-interaction))
    (swap-active-interaction-by-atomic active-interaction interactions)
    :else (println "Unsupported type of interaction!")
    ))

(defn- contains-value?
  "Does the vector contain a value?"
  [element coll]
  (boolean (some #(= element %) coll)))

(defn- is-valid-interaction?
  "Is the given interaction valid compared to the active-interaction of the monitor"
  [sender receivers label active-interaction]
  (and
    (and (if (instance? Seqable label)
           (or (contains-value? (:action active-interaction) label) (= label (:action active-interaction)))
           (or (= label (:action active-interaction)) (contains-value? label (:action active-interaction)))))
    (= sender (:sender active-interaction))
    (and (if (instance? Seqable (:receiver active-interaction))
           (or (contains-value? receivers (:receiver active-interaction)) (= receivers (:receiver active-interaction)))
           (or (= receivers (:receiver active-interaction)) (contains-value? (:receiver active-interaction) receivers))))))

(defn is-valid-communication?
  "Checks if communication is valid by comparing input to the active monitor"
  [sender receivers label active-interaction]
  (cond
    (instance? interaction @active-interaction)
    (is-valid-interaction? sender receivers label @active-interaction)
    :else
    (do (println "Communication invalid!")
        false)))

(defn equal-monitors?
  "Check if all channels have the same monitor"
  [channels]
  (= 1 (count (distinct (for [c channels] (get-monitor-id (get-monitor c)))))))

(defrecord monitor [id interactions active-interaction]
  monitoring
  (get-monitor-id [this] id)
  (get-active-interaction [this] @active-interaction)
  (apply-interaction [this label] (apply-interaction-to-mon label active-interaction interactions))
  (valid-interaction? [this sender receivers label] (is-valid-communication? sender receivers label active-interaction)))
