(in-ns 'discourje.core.async.async)

(defprotocol monitoring
  (get-active-interaction [this])
  (apply-interaction [this label]))

(defrecord monitor [interactions channels active-interaction]
  monitoring
  (get-active-interaction [this] @active-interaction)
  (apply-interaction [this label] (apply-interaction label active-interaction interactions)))

(defn- check-atomic-interaction [label active-interaction]
  (= (get-action @active-interaction) label))

(defn- swap-active-interaction-by-atomic [active-interaction interactions]
  )

(defn- apply-interaction [label active-interaction interactions]
  (cond
    (and (instance? interaction @active-interaction) (check-atomic-interaction label active-interaction))
    ))
