;monitoring.clj
(in-ns 'discourje.core.async.async)

(defprotocol monitoring
  (get-monitor-id [this])
  (get-active-interaction [this])
  ;(send-interaction [this label])
  (apply-interaction [this sender receivers label])
  (valid-interaction? [this sender receivers label]))

(defn- check-atomic-interaction
  "Check the atomic interaction"
  [label active-interaction]
  (= (get-action @active-interaction) label))

(defn- check-branch-interaction
  "Check the atomic interaction"
  [label active-interaction]
  (> (count (filter (fn [x] (= x label)) (flatten (for [b (:branches active-interaction)] (get-action (nth b 0)))))) 0))

(defn- swap-next-interaction!
  "Get the next interaction"
  [interactions]
  (fn [active-interaction]
    (first (filter
             (fn [inter]
               (cond (satisfies? interactable active-interaction) (= (get-id inter) (get-next active-interaction))
                     :else (do (println "Not supported type!") false)))
             interactions))))

(defn- swap-next-interaction-by-id!
  "Get the next interaction with next id already given"
  [id interactions]
  (fn [active-interaction]
    (first (filter
             (fn [inter]
               (cond (satisfies? interactable active-interaction) (= (get-id inter) id)
                     (satisfies? branch active-interaction) (swap-next-interaction-by-id! id interactions)
                     :else (do (println "Not supported type!") false)))
             interactions))))

(defn- multiple-receivers?
  "Does the monitor have multiple receivers?"
  [active-interaction]
  (println (format "Checking multiple-receivers on active-interaction %s, seqable? %s, count > 1 %s"
                   active-interaction
                   (instance? Seqable (:receivers active-interaction))
                   (> (count (:receivers active-interaction)) 1)))
  (and (instance? Seqable (:receivers active-interaction)) (> (count (:receivers active-interaction)) 1)))

(defn- remove-receiver-from-branch
  "Remove a receiver from the active monitor when in first position of a branch"
  [active-interaction target-interaction receiver]
  (let [recv (:receivers target-interaction)
        newRecv (vec (remove #{receiver} recv))]
    (println (format "removing receiver %s, new receivers collection: %s" receiver newRecv))
    (cond
      (satisfies? interactable target-interaction)
      (swap! active-interaction (fn [inter] (->interaction (get-id inter) (get-action inter) (get-sender inter) newRecv (get-next inter)))))))

(defn- remove-receiver
  "Remove a receiver from the active monitor"
  [active-interaction receiver]
  (let [currentMonitor @active-interaction
        recv (:receivers currentMonitor)
        newRecv (vec (remove #{receiver} recv))]
    (println (format "removing receiver %s, new receivers collection: %s" receiver newRecv))
    (cond
      (satisfies? interactable currentMonitor)
      (swap! active-interaction (fn [inter] (->interaction (:id inter) (:action inter) (:sender inter) newRecv (:next inter)))))))

(defn- swap-active-interaction-by-atomic
  "Swap active interaction by atomic"
  ([active-interaction receiver interactions]
   (if (nil? receiver)
     (swap-active-interaction-by-atomic active-interaction interactions)
     (if (multiple-receivers? @active-interaction)
       (remove-receiver active-interaction receiver)
       (swap! active-interaction (swap-next-interaction! interactions)))))
  ([active-interaction interactions]
   ((swap! active-interaction (swap-next-interaction! interactions)))))

(defn get-first-valid-target-branch-interaction
  "Find the first interactable in (nested) branch constructs."
  [sender receiver label active-interaction]
  (first (filter (fn [branch]
                   (let [inter (nth branch 0)]
                     (cond
                       (satisfies? interactable inter) (and (= (:action inter) label) (= (:receivers inter) receiver) (= (:sender inter) sender))
                       (satisfies? branch inter) (get-first-valid-target-branch-interaction active-interaction label sender receiver)
                       :else (println "Not supported get nested branch!"))))
                 (:branches active-interaction))))

(defn- swap-active-interaction-by-branch
  "Swap active interaction by branch"
  [sender receivers label active-interaction interactions]
  (let [target-interaction (get-first-valid-target-branch-interaction sender receivers label @active-interaction)]
    (if (multiple-receivers? target-interaction)
      (remove-receiver-from-branch active-interaction target-interaction receivers)
      (swap! active-interaction (swap-next-interaction-by-id! (:next target-interaction) interactions)))))

(defn- apply-interaction-to-mon
  "Apply new interaction"
  [sender receivers label active-interaction interactions]
  (println (format "Applying: label %s, receiver %s." label receivers))
  (cond
    (and (satisfies? interactable @active-interaction) (check-atomic-interaction label active-interaction))
    (swap-active-interaction-by-atomic active-interaction receivers interactions)
    (and (satisfies? branch @active-interaction) (check-branch-interaction label @active-interaction))
    (swap-active-interaction-by-branch sender receivers label active-interaction interactions)
    :else (println "Unsupported type of interaction!")
    ))

(defn- contains-value?
  "Does the vector contain a value?"
  [element coll]
  (boolean (some #(= element %) coll)))

(defn- is-valid-interaction?
  "Is the given interaction valid compared to the active-interaction of the monitor"
  [sender receivers label active-interaction]
  (println (format "input = %s %s %s" sender receivers label))
  (println (format "active  = %s %s %s" (:sender active-interaction) (:receivers active-interaction) (:action active-interaction)))
  (and
    (and (if (instance? Seqable label)
           (or (contains-value? (:action active-interaction) label) (= label (:action active-interaction)))
           (or (= label (:action active-interaction)) (contains-value? label (:action active-interaction)))))
    (= sender (:sender active-interaction))
    (and (if (instance? Seqable (:receivers active-interaction))
           (or (contains-value? receivers (:receivers active-interaction)) (= receivers (:receivers active-interaction)))
           (or (= receivers (:receivers active-interaction)) (contains-value? (:receivers active-interaction) receivers))))))

(defn is-valid-communication?
  "Checks if communication is valid by comparing input to the active monitor"
  [sender receivers label active-interaction]
  (cond
    (satisfies? interactable active-interaction)
    (is-valid-interaction? sender receivers label active-interaction)
    (satisfies? branch active-interaction)
    (> (count (filter true? (flatten (for [b (:branches active-interaction)] (is-valid-communication? sender receivers label (nth b 0)))))) 0)
    :else
    (do (println "Unsupported communication type: Communication invalid, " active-interaction)
        false)))

(defn equal-monitors?
  "Check if all channels have the same monitor"
  [channels]
  (= 1 (count (distinct (for [c channels] (get-monitor-id (get-monitor c)))))))

(defrecord monitor [id interactions active-interaction]
  monitoring
  (get-monitor-id [this] id)
  (get-active-interaction [this] @active-interaction)
  (apply-interaction [this sender receivers label] (apply-interaction-to-mon sender receivers label active-interaction interactions))
  (valid-interaction? [this sender receivers label] (is-valid-communication? sender receivers label @active-interaction)))

