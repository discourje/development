;monitoring.clj
(in-ns 'discourje.core.async)

;forward declare check-branchable-interaction to resolve undefined issue in check-recursion-interaction
(declare check-branch-interaction get-branch-interaction)

(defprotocol monitoring
  (get-monitor-id [this])
  (get-active-interaction [this])
  (apply-interaction [this sender receivers label])
  (valid-interaction? [this sender receivers label])
  (is-current-multicast? [this label])
  (register-rec! [this rec])
  (get-rec [this name]))

(declare contains-value? is-valid-interaction?)

(defn- interaction-to-string
  "Stringify an interaction, returns empty string if the given interaction is nil"
  [interaction]
  (if (nil? interaction) "" (to-string interaction)))

(defn- check-recursion-interaction
  "Check the first element in a recursion interaction"
  [sender receivers label active-interaction]
  (let [rec (get-recursion active-interaction)
        first-interaction (first rec)]
    (cond
      (satisfies? interactable first-interaction) (is-valid-interaction? sender receivers label first-interaction)
      (satisfies? branchable first-interaction) (check-branch-interaction sender receivers label first-interaction)
      (satisfies? recursable first-interaction) (check-recursion-interaction sender receivers label first-interaction)
      :else (log-error :unsupported-operation "No correct next recursion monitor found" (interaction-to-string first-interaction)))))

(defn- check-branch-interaction
  "Check the atomic interaction"
  [sender receivers label active-interaction]
  (> (count (filter (fn [x] (true? x))
                    (flatten
                      (for [first-in-branch (:branches active-interaction)]
                        (cond
                          (satisfies? interactable first-in-branch) (is-valid-interaction? sender receivers label first-in-branch)
                          (satisfies? branchable first-in-branch) (check-branch-interaction sender receivers label first-in-branch)
                          (satisfies? recursable first-in-branch) (check-recursion-interaction sender receivers label first-in-branch)
                          :else (log-error :unsupported-operation "No correct next monitor found in first position of a branchable construct!" (interaction-to-string first-in-branch)))))))
     0))

(defn- multiple-receivers?
  "Does the monitor have multiple receivers?"
  [active-interaction]
  (log-message (format "Checking multiple-receivers on active-interaction %s, seqable? %s, count > 1 %s"
                       active-interaction
                       (instance? Seqable (:receivers active-interaction))
                       (and (instance? Seqable (:receivers active-interaction)) (> (count (:receivers active-interaction)) 1))))
  (and (instance? Seqable (:receivers active-interaction)) (> (count (:receivers active-interaction)) 1)))

(defn- remove-receiver-from-branch
  "Remove a receiver from the active monitor when in first position of a branchable"
  [active-interaction target-interaction receiver]
  (let [recv (:receivers target-interaction)
        newRecv (vec (remove #{receiver} recv))]
    (log-message (format "IN-BRANCH!! removing receiver %s, new receivers collection: %s" receiver newRecv))
    (cond
      (satisfies? interactable target-interaction)
      (swap! active-interaction (fn [inter]
                                  (log-message (format "STILL HAS MULTIPLE RECEIVERS In First Of Branch? %s | %s && ID = SAME %s? Active: %s, Current: %s" (multiple-receivers? @active-interaction) (multiple-receivers? target-interaction) (= (get-id @active-interaction) (get-id target-interaction)) @active-interaction target-interaction))
                                  (if (or (satisfies? identifiable-recur @active-interaction) (satisfies? branchable @active-interaction) (and (multiple-receivers? @active-interaction) (= (get-id @active-interaction) (get-id target-interaction))))
                                    (->interaction (:id target-interaction) (:action target-interaction) (:sender target-interaction) (vec (remove #{receiver} (:receivers @active-interaction))) (:next target-interaction))
                                    (get-next target-interaction)))))))

(defn- remove-receiver
  "Remove a receiver from the active monitor"
  [active-interaction current-interaction receiver]
  (let [recv (:receivers current-interaction)
        newRecv (vec (remove #{receiver} recv))]
    (log-message (format "removing receiver %s, new receivers collection: %s" receiver newRecv))
    (if (satisfies? interactable current-interaction)
      (swap! active-interaction (fn [inter]
                                  (log-message (format "STILL HAS MULTIPLE RECEIVERS? %s | %s && ID = SAME %s? Active: %s, Current: %s" (multiple-receivers? @active-interaction) (multiple-receivers? current-interaction) (= (get-id @active-interaction) (get-id current-interaction)) @active-interaction current-interaction))
                                  (if (or (satisfies? identifiable-recur @active-interaction) (and (multiple-receivers? @active-interaction) (= (get-id @active-interaction) (get-id current-interaction))))
                                    (->interaction (:id current-interaction) (:action current-interaction) (:sender current-interaction) (vec (remove #{receiver} (:receivers @active-interaction))) (:next current-interaction))
                                    (if (not= nil (get-next current-interaction))
                                      (get-next current-interaction)
                                      nil))))
      (log-error :unsupported-operation (format "Cannot remove-receiver from interaction of type: %s, it should be atomic! Interaction = %s" (type current-interaction) (interaction-to-string current-interaction))))))

(defn- swap-active-interaction-by-atomic
  "Swap active interaction by atomic"
  [active-interaction target-interaction receiver]
  (if (nil? receiver)
    (swap! active-interaction (fn [x] (get-next target-interaction)))
    (if (multiple-receivers? target-interaction)
      (remove-receiver active-interaction target-interaction receiver)
      (reset! active-interaction (if (not= nil (get-next target-interaction))
                                   (get-next target-interaction)
                                   nil)))))

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

(defn- get-atomic-interaction
  "Check the atomic interaction"
  [sender receiver label active-interaction]
  (when (is-valid-interaction? sender receiver label active-interaction) active-interaction))

(defn- get-recursion-interaction
  "Check the first element in a recursion interaction"
  [sender receiver label active-interaction]
  (let [rec (get-recursion active-interaction)]
    (cond
      (satisfies? interactable rec) (get-atomic-interaction sender receiver label rec)
      (satisfies? branchable rec) (get-branch-interaction sender receiver label rec)
      (satisfies? recursable rec) (get-recursion-interaction sender receiver label rec)
      :else (log-error :unsupported-operation (format "No correct next recursion monitor found. %s" (interaction-to-string rec))))))

(defn- get-branch-interaction
  "Check the atomic interaction"
  [sender receiver label active-interaction]
  (flatten
    (for [branch (:branches active-interaction)]
      (cond
        (satisfies? interactable branch) (get-atomic-interaction sender receiver label branch)
        (satisfies? branchable branch) (get-branch-interaction sender receiver label branch)
        (satisfies? recursable branch) (get-recursion-interaction sender receiver label branch)
        :else (log-error :unsupported-operation (format "Cannot check operation on child branchable construct! %s" (interaction-to-string branch)))))))


(defn- get-first-valid-target-branch-interaction
  "Find the first interactable in (nested) branchable constructs."
  [sender receiver label active-interaction]
  (first (filter some? (get-branch-interaction sender receiver label active-interaction))))

(defn- swap-active-interaction-by-branch
  "Swap active interaction by branchable"
  [sender receivers label active-interaction target-interaction]
  (let [target (get-first-valid-target-branch-interaction sender receivers label target-interaction)]
    (log-message (format "target sender %s receivers %s action %s next %s or is identifiable-recur %s" (:sender target) (:receivers target) (:action target) (:next target) (satisfies? identifiable-recur target)))
    (if (multiple-receivers? target)
      (remove-receiver-from-branch active-interaction target receivers)
      (swap! active-interaction (fn [x] (:next target))))))

(defn- swap-active-interaction-by-recursion
  "Swap active interaction bu recursion"
  [sender receivers label active-interaction target-interaction]
  (cond (satisfies? interactable target-interaction)
        (if (nil? receivers)
          (swap! active-interaction (fn [x] (get-next target-interaction)))
          (if (multiple-receivers? target-interaction)
            (remove-receiver active-interaction target-interaction receivers)
            (swap! active-interaction (fn [x] (get-next target-interaction)))))
        (satisfies? branchable target-interaction)
        (let [first-in-branch (get-first-valid-target-branch-interaction sender receivers label target-interaction)]
          (log-message (format "first-in-branchable sender %s receivers %s action %s next %s, or is identifiable-recur %s" (:sender first-in-branch) (:receivers first-in-branch) (:action first-in-branch) (:next first-in-branch) (satisfies? identifiable-recur first-in-branch)))
          (if (multiple-receivers? first-in-branch)
            (remove-receiver-from-branch active-interaction first-in-branch receivers)
            (swap! active-interaction (fn [x] (:next first-in-branch)))))
        (satisfies? recursable target-interaction)
        (swap-active-interaction-by-recursion sender receivers label active-interaction (get-recursion target-interaction))
        :else (log-error :unsupported-operation (format "Cannot update the interaction, unknown type: %s!" (type target-interaction)))))

(defn add-rec-to-table
  "Add a recursion to rectable for continues to query"
  [rec-set rec]
  (when (nil? ((get-name rec) @rec-set))
    (swap! rec-set assoc (get-name rec) rec)))

(defn- apply-interaction-to-mon
  "Apply new interaction"
  ([monitor sender receivers label active-interaction target-interaction]
   (log-message (format "Applying: label %s, receiver %s." label receivers))
   (cond
     (satisfies? interactable target-interaction)
     (swap-active-interaction-by-atomic active-interaction target-interaction receivers)
     (satisfies? branchable target-interaction)
     (swap-active-interaction-by-branch sender receivers label active-interaction target-interaction)
     (satisfies? recursable target-interaction)
     (swap-active-interaction-by-recursion sender receivers label active-interaction target-interaction)
     (satisfies? identifiable-recur target-interaction)
     (apply-interaction-to-mon monitor sender receivers label active-interaction (get-rec monitor (get-name target-interaction)))
     :else (log-error :unsupported-operation (format "Unsupported type of interaction to apply %s!" (type target-interaction)))
     )))

(defn- contains-value?
  "Does the vector contain a value?"
  [element coll]
  (when (instance? Seqable coll)
    (boolean (some #(= element %) coll))))

(defn is-valid-communication?
  "Checks if communication is valid by comparing input to the active monitor"
  [monitor sender receivers label active-interaction]
  (cond
    (satisfies? interactable active-interaction)
    (is-valid-interaction? sender receivers label active-interaction)
    (satisfies? branchable active-interaction)
    (> (count (filter true? (flatten (for [b (:branches active-interaction)] (is-valid-communication? monitor sender receivers label b))))) 0)
    (satisfies? recursable active-interaction)
    (do (register-rec! monitor active-interaction)
        (is-valid-communication? monitor sender receivers label (get-recursion active-interaction)))
    (satisfies? identifiable-recur active-interaction)
    (is-valid-communication? monitor sender receivers label (get-rec monitor (get-name active-interaction)))
    :else
    (do (log-error :unsupported-operation (format "Unsupported communication type: Communication invalid, type: %s" (type active-interaction)))
        false)))

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
    (satisfies? recursable active-interaction)
    (is-active-interaction-multicast? monitor (get-recursion active-interaction) label)
    (satisfies? identifiable-recur active-interaction)
    (is-active-interaction-multicast? monitor (get-rec monitor (get-name (get-next active-interaction))) label)
    :else
    (do (log-error :unsupported-operation (format "Unsupported communication type: Communication invalid, type: %s" (type active-interaction)))
        false))
  )

(defn force-monitor-reset! "Force the monitor to go back to the first interaction." [monitor]
  (reset! (:active-interaction monitor) (:interactions monitor)))

(defrecord monitor [id interactions active-interaction recursion-set]
  monitoring
  (get-monitor-id [this] id)
  (get-active-interaction [this] @active-interaction)
  (apply-interaction [this sender receivers label] (apply-interaction-to-mon this sender receivers label active-interaction @active-interaction))
  (valid-interaction? [this sender receivers label] (is-valid-communication? this sender receivers label @active-interaction))
  (is-current-multicast? [this label] (is-active-interaction-multicast? this @active-interaction label))
  (register-rec! [this rec] (add-rec-to-table recursion-set rec))
  (get-rec [this name] (name @recursion-set)))