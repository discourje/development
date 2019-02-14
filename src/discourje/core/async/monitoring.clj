(in-ns 'discourje.core.async.async)

(defprotocol monitoring
  (get-active-interaction [this])
  (apply-interaction [this label]))

(defn- check-atomic-interaction
  "Check the atomic interaction"
  [label active-interaction]
  (= (get-action @active-interaction) label))

(defn- interaction-filter
  "Filter interactions"
  [active-interaction interactions]
  (filter
    (fn [interaction]
      (cond (instance? interaction @active-interaction) (when (= (get-id interaction) (get-next active-interaction) interaction))
            :else (println "Not supported type!"))
      interactions)))

(defn- get-next-interaction
  "Get the next interaction"
  [active-interaction interactions]
  (first(vec (some? (interaction-filter active-interaction interactions)))))

(defn- swap-active-interaction-by-atomic
  "Swap active interaction by atomic"
  [active-interaction interactions]
  (swap! active-interaction (get-next-interaction active-interaction interactions)))

(defn- apply-interaction-to-mon
  "Apply new interaction"
  [label active-interaction interactions]
  (cond
    (and (instance? interaction @active-interaction) (check-atomic-interaction label active-interaction))
    (swap-active-interaction-by-atomic active-interaction interactions)
    :else (println "Unsupported type!")
    ))

(defrecord monitor [interactions active-interaction]
  monitoring
  (get-active-interaction [this] @active-interaction)
  (apply-interaction [this label] (apply-interaction-to-mon label active-interaction interactions)))

