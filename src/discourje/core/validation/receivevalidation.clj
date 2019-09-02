;receivevalidation.clj
(in-ns 'discourje.core.async)

;forward declare check-branchable-interaction to resolve undefined issue in check-recursion-interaction
(declare check-branch-interaction check-parallel-interaction get-branch-interaction get-first-valid-target-branch-interaction get-parallel-interaction is-valid-interaction? interaction-to-string is-active-interaction-multicast? add-rec-to-table)

(defn- check-recursion-interaction
  "Check the first element in a recursion interaction"
  [sender receivers label active-interaction]
  (let [rec (get-recursion active-interaction)
        first-interaction (first rec)]
    (cond
      (satisfies? interactable first-interaction) (is-valid-interaction? sender receivers label first-interaction)
      (satisfies? branchable first-interaction) (check-branch-interaction sender receivers label first-interaction)
      (satisfies? recursable first-interaction) (check-recursion-interaction sender receivers label first-interaction)
      (satisfies? parallelizable parallel) (check-parallel-interaction sender receivers label parallel)
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
                          (satisfies? parallelizable parallel) (check-parallel-interaction sender receivers label parallel)
                          :else (log-error :unsupported-operation "No correct next monitor found in first position of a branchable construct!" (interaction-to-string first-in-branch)))))))
     0))
(defn- check-parallel-interaction
  "Check the atomic interaction"
  [sender receivers label active-interaction]
  (> (count (filter (fn [x] (true? x))
                    (flatten
                      (for [parallel (get-parallel active-interaction)]
                        (cond
                          (satisfies? interactable parallel) (is-valid-interaction? sender receivers label parallel)
                          (satisfies? branchable parallel) (check-branch-interaction sender receivers label parallel)
                          (satisfies? recursable parallel) (check-recursion-interaction sender receivers label parallel)
                          (satisfies? parallelizable parallel) (check-parallel-interaction sender receivers label parallel)
                          :else (log-error :unsupported-operation "No correct next monitor found in first position of a branchable construct!" (interaction-to-string parallel)))))))
     0))

(defn- multiple-receivers?
  "Does the monitor have multiple receivers?"
  [active-interaction]
  (log-message (format "Checking multiple-receivers on active-interaction %s, seqable? %s, count > 1 %s"
                       active-interaction
                       (instance? Seqable (:receivers active-interaction))
                       (and (instance? Seqable (:receivers active-interaction)) (> (count (:receivers active-interaction)) 1))))
  (and (instance? Seqable (:receivers active-interaction)) (> (count (:receivers active-interaction)) 1)))


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

(defn- swap-active-interaction-by-atomic
  "Swap active interaction by atomic"
  [active-interaction target-interaction receiver]
  (println (format "swapping %s with %s" (to-string @active-interaction) (to-string target-interaction)))
  (let [pre-swap-interaction @active-interaction]
    (if (multiple-receivers? target-interaction)
      (remove-receiver active-interaction target-interaction receiver)
      (= (get-id (swap! active-interaction (fn [inter]
                                             (if (= (get-id inter) (get-id pre-swap-interaction))
                                               (if (not= nil (get-next target-interaction))
                                                 (get-next target-interaction)
                                                 nil)
                                               inter))))
         (get-id target-interaction)))))


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
      (satisfies? branchable rec) (get-first-valid-target-branch-interaction sender receiver label rec)
      (satisfies? recursable rec) (get-recursion-interaction sender receiver label rec)
      (satisfies? parallelizable rec) (get-parallel-interaction sender receiver label rec)
      :else (log-error :unsupported-operation (format "No correct next recursion monitor found. %s" (interaction-to-string rec))))))

(defn- get-parallel-interaction
  "Check the atomic interaction"
  [sender receiver label active-interaction]
  (when-let [_ (for [parallel (get-parallel active-interaction)]
                 (cond
                   (satisfies? interactable parallel) (get-atomic-interaction sender receiver label parallel)
                   (satisfies? branchable parallel) (get-first-valid-target-branch-interaction sender receiver label parallel)
                   (satisfies? parallelizable parallel) (get-parallel-interaction sender receiver label parallel)
                   (satisfies? recursable parallel) (get-recursion-interaction sender receiver label parallel)
                   :else (log-error :unsupported-operation (format "Cannot check operation on child parallel construct! %s" (interaction-to-string parallel))))
                 )]
    active-interaction))

(defn- get-branch-interaction
  "Check the atomic interaction"
  [sender receiver label active-interaction]
  (flatten
    (for [branch (:branches active-interaction)]
      (cond
        (satisfies? interactable branch) (get-atomic-interaction sender receiver label branch)
        (satisfies? branchable branch) (get-first-valid-target-branch-interaction sender receiver label branch)
        (satisfies? parallelizable branch) (get-parallel-interaction sender receiver label branch)
        (satisfies? recursable branch) (get-recursion-interaction sender receiver label branch)
        :else (log-error :unsupported-operation (format "Cannot check operation on child branchable construct! %s" (interaction-to-string branch)))))))

(defn- get-first-valid-target-parallel-interaction
  "Find the first interactable in (nested) parallel constructs."
  [sender receiver label active-interaction]
  (get-parallel-interaction sender receiver label active-interaction))

(defn- get-first-valid-target-branch-interaction
  "Find the first interactable in (nested) branchable constructs."
  [sender receiver label active-interaction]
  (first (filter some? (get-branch-interaction sender receiver label active-interaction))))

(defn- remove-from-nested-parallel [target par monitor]
  (cond
    (= (get-id par) (get-id target))
    true
    (satisfies? branchable par)
    (first (filter true? (for [b (get-branches par)] (remove-from-nested-parallel target b monitor))))
    (satisfies? recursable par)
    (remove-from-nested-parallel target (get-recursion par) monitor)
    (satisfies? parallelizable par)
    (first (filter true? (for [p (get-parallel par)] (remove-from-nested-parallel target p monitor))))
    (satisfies? identifiable-recur par)
    (remove-from-nested-parallel target (get-rec monitor (get-name par)) monitor)
    ))

(defn remove-nested-parallel [sender receivers label target-interaction monitor]
  (let [pars (flatten (filter some?
                              (for [par (get-parallel target-interaction)]
                                (let [inter (cond
                                              (satisfies? parallelizable par)
                                              (remove-nested-parallel sender receivers label par monitor)
                                              (satisfies? interactable par)
                                              (get-atomic-interaction sender receivers label par)
                                              (satisfies? branchable par)
                                              (get-first-valid-target-branch-interaction sender receivers label par)
                                              (satisfies? recursable par)
                                              (get-recursion-interaction sender receivers label par)
                                              (satisfies? identifiable-recur par)
                                              (let [recursion (get-rec monitor (get-name par))
                                                    valid-rec (get-recursion-interaction sender receivers label recursion)]
                                                (if (nil? valid-rec)
                                                  par
                                                  recursion))
                                              :else
                                              par
                                              )]
                                  (if (nil? inter)
                                    (if (satisfies? parallelizable par)
                                      nil
                                      par)
                                    (cond
                                      (satisfies? parallelizable inter)
                                      (remove-nested-parallel sender receivers label inter monitor)
                                      (satisfies? interactable inter)
                                      (get-next inter)
                                      (or (satisfies? recursable inter) (satisfies? branchable inter))
                                      inter
                                      (and (instance? clojure.lang.LazySeq inter) (not (satisfies? interactable inter)))
                                      (first (filter some? inter))
                                      ))
                                  ))))]
    (if (empty? pars)
      (get-next target-interaction)
      (assoc target-interaction :parallels pars))))

(defn- swap-active-interaction-by-parallel
  "Swap active interaction by parallel"
  [sender receivers label active-interaction target-interaction monitor]
  (let [target-parallel-interaction (get-first-valid-target-parallel-interaction sender receivers label target-interaction)]
    (log-message (format "target sender %s receivers %s action %s next %s or is identifiable-recur %s" (:sender target-parallel-interaction) (:receivers target-parallel-interaction) (:action target-parallel-interaction) (:next target-parallel-interaction) (satisfies? identifiable-recur target-parallel-interaction)))
    (if (multiple-receivers? target-parallel-interaction)
      (remove-receiver-from-parallel active-interaction target-parallel-interaction receivers)
      (swap! active-interaction
             (fn [inter]
               (let [target (if (= (get-id inter) (get-id target-parallel-interaction))
                              (get-first-valid-target-parallel-interaction sender receivers label inter)
                              target-parallel-interaction)]
                 (if (nil? target)
                   inter
                   (if (satisfies? parallelizable target)
                     (let [par-target (remove-nested-parallel sender receivers label target monitor)]
                       par-target)
                     (let [parallel-with-removed-par (remove (fn [x] (remove-from-nested-parallel target x monitor)) (get-parallel inter))]
                       (if (and (empty? parallel-with-removed-par) (nil? (get-next target)))
                         (if (nil? (get-next target))
                           (assoc inter :parallels parallel-with-removed-par)
                           (assoc inter :parallels (conj parallel-with-removed-par (get-next target))))))))))))
    true))


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

(defn- apply-receive-to-mon
  "Apply new interaction"
  ([monitor sender receivers label active-interaction target-interaction]
   (log-message (format "Applying: RECEIVE label %s, receiver %s." label receivers))
   (cond
     (satisfies? interactable target-interaction)
     (swap-active-interaction-by-atomic active-interaction target-interaction receivers)
     (satisfies? branchable target-interaction)
     (apply-receive-to-mon monitor sender receivers label active-interaction (get-first-valid-target-branch-interaction sender receivers label target-interaction))
     (satisfies? parallelizable target-interaction)
     (swap-active-interaction-by-parallel sender receivers label active-interaction target-interaction monitor)
     (satisfies? recursable target-interaction)
     (apply-receive-to-mon monitor sender receivers label active-interaction (get-recursion target-interaction))
     (satisfies? identifiable-recur target-interaction)
     (apply-receive-to-mon monitor sender receivers label active-interaction (get-rec monitor (get-name target-interaction)))
     :else (log-error :unsupported-operation (format "Unsupported type of interaction to apply %s!" (type target-interaction)))
     )))