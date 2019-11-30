;monitoring.clj
(in-ns 'discourje.core.async)

(defn equal-monitors?
  "Check if all channels have the same monitor"
  [channels]
  (= 1 (count (distinct (for [c channels] (get-monitor-id (get-monitor c)))))))

(defn force-monitor-reset! "Force the monitor to go back to the first interaction." [monitor interactions]
  (reset! (:active-interaction monitor) interactions))

(defn- get-rec-from-table [name rec-table save-mapping]
  (if (vector? name)
    (let [entry ((first name) @rec-table)
          mapping (second name)
          new-mapping (create-new-mapping (get-current-mapping entry) mapping)
          new-recursion (get-mapped-rec entry mapping)]
      ;(println save-mapping)
      ;(println "oldmapping = "  (get-current-mapping entry) )
      (when (true? save-mapping)
        (do; (println "mapping to set"mapping)
          ;(println "new mapping="new-mapping)
            (swap! rec-table assoc (first name) (assoc entry :initial-mapping new-mapping))
            ;(reset! rec-table (assoc @rec-table (first name) (assoc entry :initial-mapping new-mapping)))
            ))
      new-recursion)
    (get-mapped-rec (name @rec-table) nil)))

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
  (is-current-multicast? [this message] (is-multicast? @active-interaction this message))
  (get-rec [this name save-mapping] (get-rec-from-table name recursion-set save-mapping))
  (valid-close? [this sender receiver] (let [pre-swap @active-interaction]
                                         (->swappable-interaction pre-swap (is-valid-closable? pre-swap this sender receiver))))
  (apply-close! [this target-interaction pre-swap-interaction channel] (apply-closable! target-interaction pre-swap-interaction active-interaction this channel)))