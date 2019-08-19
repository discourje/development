;monitoring.clj
(in-ns 'discourje.core.async)

;forward declare check-branchable-interaction to resolve undefined issue in check-recursion-interaction
(declare check-branch-interaction get-branch-interaction get-send-branch-interaction get-parallel-interaction get-send-parallel-interaction)

(defprotocol monitoring
  (get-monitor-id [this])
  (get-active-interaction [this])
  (apply-receive! [this sender receivers label])
  (apply-send! [this sender receivers label])
  (valid-send? [this sender receivers label])
  (valid-receive? [this sender receivers label])
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
                                  (log-message (format "STILL HAS MULTIPLE RECEIVERS In First Of Branch? %s | %s && ID = SAME %s? Active: %s, Current: %s" (multiple-receivers? inter) (multiple-receivers? target-interaction) (= (get-id inter) (get-id target-interaction)) inter target-interaction))
                                  (if (or (satisfies? identifiable-recur inter) (satisfies? branchable inter) (and (multiple-receivers? inter) (= (get-id inter) (get-id target-interaction))))
                                    (->interaction (:id target-interaction) (:action target-interaction) (:sender target-interaction) (vec (remove #{receiver} (:receivers inter))) (:accepted-sends target-interaction) (:next target-interaction))
                                    (get-next target-interaction)))))))

(defn- remove-receiver-from-parallel
  "Remove a receiver from the active monitor when in first position of a branchable"
  [active-interaction target-interaction receiver]
  (let [recv (:receivers target-interaction)
        newRecv (vec (remove #{receiver} recv))]
    (log-message (format "IN-PARALLEL!! removing receiver %s, new receivers collection: %s" receiver newRecv))
    (swap! active-interaction
           (fn [inter]
             (log-message (format "STILL HAS MULTIPLE RECEIVERS In First Of Branch? %s | %s && ID = SAME %s? Active: %s, Current: %s" (multiple-receivers? inter) (multiple-receivers? target-interaction) (= (get-id inter) (get-id target-interaction)) inter target-interaction))
             (if (or (satisfies? identifiable-recur inter) (satisfies? branchable inter) (and (multiple-receivers? inter) (= (get-id inter) (get-id target-interaction))))
               (->interaction (:id target-interaction) (:action target-interaction) (:sender target-interaction) (vec (remove #{receiver} (:receivers inter))) (:accepted-sends target-interaction) (:next target-interaction))
               (get-next target-interaction))))))

(defn- remove-receiver
  "Remove a receiver from the active monitor"
  [active-interaction current-interaction receiver]
  (let [recv (:receivers current-interaction)
        newRecv (vec (remove #{receiver} recv))]
    (log-message (format "removing receiver %s, new receivers collection: %s" receiver newRecv))
    (if (satisfies? interactable current-interaction)
      (swap! active-interaction (fn [inter]
                                  (log-message (format "STILL HAS MULTIPLE RECEIVERS? %s | %s && ID = SAME %s? Active: %s, Current: %s" (multiple-receivers? inter) (multiple-receivers? current-interaction) (= (get-id inter) (get-id current-interaction)) inter current-interaction))
                                  (if (or (satisfies? identifiable-recur inter) (and (multiple-receivers? inter) (= (get-id inter) (get-id current-interaction))))
                                    (->interaction (:id current-interaction) (:action current-interaction) (:sender current-interaction) (vec (remove #{receiver} (:receivers inter))) (:accepted-sends current-interaction) (:next current-interaction))
                                    (if (not= nil (get-next current-interaction))
                                      (get-next current-interaction)
                                      nil))))
      (log-error :unsupported-operation (format "Cannot remove-receiver from interaction of type: %s, it should be atomic! Interaction = %s" (type current-interaction) (interaction-to-string current-interaction))))))

(defn- add-sender
  "add-sender from to active monitor"
  [active-interaction current-interaction sender receiver]
  (let [recv (:receivers current-interaction)
        newRecv (vec (remove #{receiver} recv))]
    (log-message (format "removing receiver %s, new receivers collection: %s" receiver newRecv))
    (if (satisfies? interactable current-interaction)
      (swap! active-interaction (fn [inter]
                                  (log-message (format "STILL HAS MULTIPLE RECEIVERS? %s | %s && ID = SAME %s? Active: %s, Current: %s" (multiple-receivers? inter) (multiple-receivers? current-interaction) (= (get-id inter) (get-id current-interaction)) inter current-interaction))
                                  (if (or (satisfies? identifiable-recur inter) (and (multiple-receivers? inter) (= (get-id inter) (get-id current-interaction))))
                                    (->interaction (:id current-interaction) (:action current-interaction) (:sender current-interaction) (vec (remove #{receiver} (:receivers inter))) (:accepted-sends current-interaction) (:next current-interaction))
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
(defn- assoc-sender-to-interaction
  "Assoc a sender to the accepted-sends set and return in"
  [inter sender]
  (assoc inter :accepted-sends (conj (:accepted-sends inter) sender)))

(defn- send-active-interaction-by-atomic
  "Send active interaction by atomic"
  [active-interaction target-interaction sender]
  (if (nil? sender)
    (do (log-error :invalid-send (format "sender appears to be nil: %s %s" active-interaction target-interaction))
        false)
    (do (swap! active-interaction (fn [inter]
                                    (if (= (get-id inter) (get-id target-interaction)) 1)
                                    (println "into sends " (assoc inter :accepted-sends (conj (:accepted-sends inter) sender)))
                                    (assoc-sender-to-interaction inter sender)))
        true)))

(defn- is-valid-interaction-for-send?
  "Check if the interaction is valid for a send operation"
  [sender receivers label active-interaction]
  (and (is-valid-interaction? sender receivers label active-interaction) (false? (contains? (get-accepted-sends active-interaction) sender))))

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

(defn- get-send-atomic-interaction
  "Check the atomic interaction"
  [sender receiver label active-interaction]
  (when (is-valid-interaction-for-send? sender receiver label active-interaction) active-interaction))

(defn- get-recursion-interaction
  "Check the first element in a recursion interaction"
  [sender receiver label active-interaction]
  (let [rec (get-recursion active-interaction)]
    (cond
      (satisfies? interactable rec) (get-atomic-interaction sender receiver label rec)
      (satisfies? branchable rec) (get-branch-interaction sender receiver label rec)
      (satisfies? recursable rec) (get-recursion-interaction sender receiver label rec)
      (satisfies? parallelizable rec) (get-parallel-interaction sender receiver label rec)
      :else (log-error :unsupported-operation (format "No correct next recursion monitor found. %s" (interaction-to-string rec))))))

(defn- get-send-recursion-interaction
  "Check the first element in a recursion interaction"
  [sender receiver label active-interaction]
  (let [rec (get-recursion active-interaction)]
    (cond
      (satisfies? interactable rec) (get-send-atomic-interaction sender receiver label rec)
      (satisfies? branchable rec) (get-send-branch-interaction sender receiver label rec)
      (satisfies? recursable rec) (get-send-recursion-interaction sender receiver label rec)
      (satisfies? parallelizable rec) (get-send-parallel-interaction sender receiver label rec)
      :else (log-error :unsupported-operation (format "No correct next recursion monitor found. %s" (interaction-to-string rec))))))

(defn- get-parallel-interaction
  "Check the atomic interaction"
  [sender receiver label active-interaction]
  (flatten
    (for [parllel (get-parallel active-interaction)]
      (cond
        (satisfies? interactable parllel) (get-atomic-interaction sender receiver label parllel)
        (satisfies? branchable parllel) (get-branch-interaction sender receiver label parllel)
        (satisfies? parallelizable parllel) (get-parallel-interaction sender receiver label parllel)
        (satisfies? recursable parllel) (get-recursion-interaction sender receiver label parllel)
        :else (log-error :unsupported-operation (format "Cannot check operation on child parallel construct! %s" (interaction-to-string parllel)))))))

(defn- get-send-parallel-interaction
  "Check the atomic interaction"
  [sender receiver label active-interaction]
  (flatten
    (for [parallel (get-parallel active-interaction)]
      (cond
        (satisfies? interactable parallel) (get-send-atomic-interaction sender receiver label parallel)
        (satisfies? branchable parallel) (get-send-branch-interaction sender receiver label parallel)
        (satisfies? parallelizable parallel) (get-send-parallel-interaction sender receiver label parallel)
        (satisfies? recursable parallel) (get-send-recursion-interaction sender receiver label parallel)
        :else (log-error :unsupported-operation (format "Cannot check operation on child parallel construct! %s" (interaction-to-string parallel)))))))

(defn- get-branch-interaction
  "Check the atomic interaction"
  [sender receiver label active-interaction]
  (flatten
    (for [branch (:branches active-interaction)]
      (cond
        (satisfies? interactable branch) (get-atomic-interaction sender receiver label branch)
        (satisfies? branchable branch) (get-branch-interaction sender receiver label branch)
        (satisfies? parallelizable branch) (get-parallel-interaction sender receiver label branch)
        (satisfies? recursable branch) (get-recursion-interaction sender receiver label branch)
        :else (log-error :unsupported-operation (format "Cannot check operation on child branchable construct! %s" (interaction-to-string branch)))))))

(defn- get-send-branch-interaction
  "Check the atomic interaction"
  [sender receiver label active-interaction]
  (flatten
    (for [branch (:branches active-interaction)]
      (cond
        (satisfies? interactable branch) (get-send-atomic-interaction sender receiver label branch)
        (satisfies? branchable branch) (get-send-branch-interaction sender receiver label branch)
        (satisfies? parallelizable branch) (get-send-parallel-interaction sender receiver label branch)
        (satisfies? recursable branch) (get-send-recursion-interaction sender receiver label branch)
        :else (log-error :unsupported-operation (format "Cannot check operation on child branchable construct! %s" (interaction-to-string branch)))))))

(defn- get-first-valid-target-parallel-interaction
  "Find the first interactable in (nested) parallel constructs."
  [sender receiver label active-interaction]
  (first (filter some? (get-parallel-interaction sender receiver label active-interaction))))

(defn- get-valid-send-parallel-interaction
  "Find the first interactable in (nested) parallel constructs."
  [sender receiver label active-interaction]
  (first (filter some? (get-send-parallel-interaction sender receiver label active-interaction))))

(defn- get-first-valid-target-branch-interaction
  "Find the first interactable in (nested) branchable constructs."
  [sender receiver label active-interaction]
  (first (filter some? (get-branch-interaction sender receiver label active-interaction))))

(defn- get-valid-send-branch-interaction
  "Find the first interactable in (nested) branchable constructs."
  [sender receiver label active-interaction]
  (first (filter some? (get-send-branch-interaction sender receiver label active-interaction))))

(defn- send-active-interaction-by-branch
  "Swap active interaction by branchable"
  [sender receivers label active-interaction target-interaction]
  (let [target (get-valid-send-branch-interaction sender receivers label target-interaction)]
    (log-message (format "target sender %s receivers %s action %s next %s or is identifiable-recur %s" (:sender target) (:receivers target) (:action target) (:next target) (satisfies? identifiable-recur target)))
    (swap! active-interaction (fn [x] (assoc-sender-to-interaction target sender)))))

(defn- swap-active-interaction-by-branch
  "Swap active interaction by branchable"
  [sender receivers label active-interaction target-interaction]
  (let [target (get-first-valid-target-branch-interaction sender receivers label target-interaction)]
    (log-message (format "target sender %s receivers %s action %s next %s or is identifiable-recur %s" (:sender target) (:receivers target) (:action target) (:next target) (satisfies? identifiable-recur target)))
    (if (multiple-receivers? target)
      (remove-receiver-from-branch active-interaction target receivers)
      (swap! active-interaction (fn [x] (:next target))))))

(defn- swap-active-interaction-by-parallel
  "Swap active interaction by parallel"
  [sender receivers label active-interaction target-interaction]
  (let [target (get-first-valid-target-parallel-interaction sender receivers label target-interaction)]
    (log-message (format "target sender %s receivers %s action %s next %s or is identifiable-recur %s" (:sender target) (:receivers target) (:action target) (:next target) (satisfies? identifiable-recur target)))
    (if (multiple-receivers? target)
      (remove-receiver-from-parallel active-interaction target receivers)
      (swap! active-interaction
             (fn [inter]
               ; (println "swapping parallel with target" target)
               (let [parallel-with-removed-par (remove (fn [x] (= (get-id x) (get-id target))) (get-parallel inter))]
                 (println "target = " target)
                 (println "parallel = " (get-parallel inter))
                 (println "removed = " parallel-with-removed-par)
                 (if (and (empty? parallel-with-removed-par) (nil? (get-next target)))
                   (get-next inter)
                   (if (nil? (get-next target))
                     (assoc inter :parallels parallel-with-removed-par)
                     (assoc inter :parallels (conj parallel-with-removed-par (get-next target)))))))))))

(defn- remove-from-nested-parallel [target par]
  (cond
    (= (get-id par) (get-id target))
    true
    (satisfies? branchable par)
    (first (filter true? (for [b (get-branches par)] (remove-from-nested-parallel target b))))
    (satisfies? recursable par)
    (remove-from-nested-parallel target (get-recursion par))
    (satisfies? parallelizable par)
    (first (filter true? (for [p (get-parallel par)] (remove-from-nested-parallel target p))))
    ))

(defn- send-active-interaction-by-parallel
  "Swap active interaction by parallel"
  [sender receivers label active-interaction target-interaction]
  (let [target (get-valid-send-parallel-interaction sender receivers label target-interaction)]
    (log-message (format "target sender %s receivers %s action %s next %s or is identifiable-recur %s" (:sender target) (:receivers target) (:action target) (:next target) (satisfies? identifiable-recur target)))
    (swap! active-interaction
           (fn [inter]
             (let [parallel-with-removed-par (remove (fn [par] (remove-from-nested-parallel target par)) (get-parallel inter))]
               (assoc inter :parallels (conj parallel-with-removed-par (assoc-sender-to-interaction target sender))))))))

(defn- swap-active-interaction-by-recursion
  "Swap active interaction bu recursion"
  [sender receivers label active-interaction target-interaction]
  (cond
    (satisfies? interactable target-interaction)
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

(defn- send-active-interaction-by-recursion
  "Swap active interaction bu recursion"
  [sender receivers label active-interaction target-interaction]
  (cond
    (satisfies? interactable target-interaction)
    (send-active-interaction-by-atomic active-interaction target-interaction sender)
    (satisfies? branchable target-interaction)
    (send-active-interaction-by-branch sender receivers label active-interaction target-interaction)
    (satisfies? parallelizable target-interaction)
    (send-active-interaction-by-parallel sender receivers label active-interaction target-interaction)
    (satisfies? recursable target-interaction)
    (send-active-interaction-by-recursion sender receivers label active-interaction (get-recursion target-interaction))
    :else (log-error :unsupported-operation (format "Cannot update the interaction, unknown type: %s!" (type target-interaction)))))

(defn add-rec-to-table
  "Add a recursion to rectable for continues to query"
  [rec-set rec]
  (when (nil? ((get-name rec) @rec-set))
    (swap! rec-set assoc (get-name rec) rec)))

(defn- apply-send-to-mon
  "Apply new interaction"
  ([monitor sender receivers label active-interaction target-interaction]
   (log-message (format "Applying: SEND label %s, receiver %s." label receivers))
   (cond
     (satisfies? interactable target-interaction)
     (send-active-interaction-by-atomic active-interaction target-interaction sender)
     (satisfies? branchable target-interaction)
     (send-active-interaction-by-branch sender receivers label active-interaction target-interaction)
     (satisfies? parallelizable target-interaction)
     (send-active-interaction-by-parallel sender receivers label active-interaction target-interaction)
     (satisfies? recursable target-interaction)
     (send-active-interaction-by-recursion sender receivers label active-interaction target-interaction)
     (satisfies? identifiable-recur target-interaction)
     (apply-send-to-mon monitor sender receivers label active-interaction (get-rec monitor (get-name target-interaction)))
     :else (log-error :unsupported-operation (format "Unsupported type of interaction to apply %s!" (type target-interaction)))
     )))

(defn- apply-receive-to-mon
  "Apply new interaction"
  ([monitor sender receivers label active-interaction target-interaction]
   (log-message (format "Applying: RECEIVE label %s, receiver %s." label receivers))
    ;(println "APPLY RECEIVE TARGET =-> "target-interaction)
   (cond
     (satisfies? interactable target-interaction)
     (swap-active-interaction-by-atomic active-interaction target-interaction receivers)
     (satisfies? branchable target-interaction)
     (swap-active-interaction-by-branch sender receivers label active-interaction target-interaction)
     (satisfies? parallelizable target-interaction)
     (swap-active-interaction-by-parallel sender receivers label active-interaction target-interaction)
     (satisfies? recursable target-interaction)
     (swap-active-interaction-by-recursion sender receivers label active-interaction target-interaction)
     (satisfies? identifiable-recur target-interaction)
     (apply-receive-to-mon monitor sender receivers label active-interaction (get-rec monitor (get-name target-interaction)))
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
    (satisfies? parallelizable active-interaction)
    (> (count (filter true? (flatten (for [p (get-parallel active-interaction)] (is-valid-communication? monitor sender receivers label p))))) 0)
    (satisfies? recursable active-interaction)
    (do (register-rec! monitor active-interaction)
        (is-valid-communication? monitor sender receivers label (get-recursion active-interaction)))
    (satisfies? identifiable-recur active-interaction)
    (is-valid-communication? monitor sender receivers label (get-rec monitor (get-name active-interaction)))
    :else
    (do (log-error :unsupported-operation (format "Unsupported communication type: Communication invalid, type: %s" (type active-interaction)))
        false)))

(defn is-valid-send-communication?
  "Checks if communication is valid by comparing input to the active monitor"
  [monitor sender receivers label active-interaction]
  (cond
    (satisfies? interactable active-interaction)
    (is-valid-interaction-for-send? sender receivers label active-interaction)
    (satisfies? branchable active-interaction)
    (> (count (filter true? (flatten (for [b (:branches active-interaction)] (is-valid-send-communication? monitor sender receivers label b))))) 0)
    (satisfies? parallelizable active-interaction)
    (> (count (filter true? (flatten (for [p (get-parallel active-interaction)] (is-valid-send-communication? monitor sender receivers label p))))) 0)
    (satisfies? recursable active-interaction)
    (do (register-rec! monitor active-interaction)
        (is-valid-send-communication? monitor sender receivers label (get-recursion active-interaction)))
    (satisfies? identifiable-recur active-interaction)
    (is-valid-send-communication? monitor sender receivers label (get-rec monitor (get-name active-interaction)))
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
  (get-rec [this name] (name @recursion-set)))