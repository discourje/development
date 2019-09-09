;receivevalidation.clj
(in-ns 'discourje.core.async)

;forward declare check-branchable-interaction to resolve undefined issue in check-recursion-interaction
(declare check-branch-interaction check-parallel-interaction get-branch-interaction get-parallel-interaction is-valid-interaction? interaction-to-string is-active-interaction-multicast? add-rec-to-table)

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

(defn- remove-receiver
  "Remove a receiver from the active monitor"
  [active-interaction current-interaction receiver]
  (let [recv (:receivers current-interaction)
        newRecv (vec (remove #{receiver} recv))]
    (log-message (format "removing receiver %s, new receivers collection: %s" receiver newRecv))
    (if (satisfies? interactable current-interaction)
      (swap! active-interaction (fn [inter]
                                  (if (= (get-id inter) (get-id current-interaction))
                                    (if (multiple-receivers? inter)
                                      (assoc inter :receivers (vec (remove #{receiver} (:receivers inter))))
                                      (if (not= nil (get-next current-interaction))
                                        (get-next current-interaction)
                                        nil))
                                    (assoc current-interaction :receivers (vec (remove #{receiver} (:receivers current-interaction)))))))
      (log-error :unsupported-operation (format "Cannot remove-receiver from interaction of type: %s, it should be atomic! Interaction = %s" (type current-interaction) (interaction-to-string current-interaction))))))

(defn- swap-active-interaction-by-atomic
  "Swap active interaction by atomic
  The end-protocol comparison indicates the protocol is terminated."
  [active-interaction target-interaction receiver]
  (let [pre-swap-interaction @active-interaction]
    (if (multiple-receivers? target-interaction)
      (remove-receiver active-interaction target-interaction receiver)
      (let [swapped (swap! active-interaction (fn [inter]
                                                (if (= (get-id inter) (get-id pre-swap-interaction))
                                                  (if (not= nil (get-next target-interaction))
                                                    (get-next target-interaction)
                                                    nil)
                                                  inter)))]
        (= (if (nil? swapped) "end-protocol" (get-id swapped))
           (if (or (nil? target-interaction) (nil? (get-next target-interaction)))
             "end-protocol"
             (get-id (get-next target-interaction))))))))


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
      (satisfies? parallelizable rec) (get-parallel-interaction sender receiver label rec)
      :else (log-error :unsupported-operation (format "No correct next recursion monitor found. %s" (interaction-to-string rec))))))

(defn- get-parallel-interaction
  "Check the atomic interaction"
  [sender receiver label active-interaction]
  (when-let [_ (for [parallel (get-parallel active-interaction)]
                 (cond
                   (satisfies? interactable parallel) (get-atomic-interaction sender receiver label parallel)
                   (satisfies? branchable parallel) (get-branch-interaction sender receiver label parallel)
                   (satisfies? parallelizable parallel) (get-parallel-interaction sender receiver label parallel)
                   (satisfies? recursable parallel) (get-recursion-interaction sender receiver label parallel)
                   :else (log-error :unsupported-operation (format "Cannot check operation on child parallel construct! %s" (interaction-to-string parallel))))
                 )]
    active-interaction))

(defn- get-branch-interaction
  "Check the atomic interaction"
  [sender receiver label active-interaction]
  (first (filter some? (flatten
                         (for [branch (:branches active-interaction)]
                           (cond
                             (satisfies? interactable branch) (get-atomic-interaction sender receiver label branch)
                             (satisfies? branchable branch) (get-branch-interaction sender receiver label branch)
                             (satisfies? parallelizable branch) (get-parallel-interaction sender receiver label branch)
                             (satisfies? recursable branch) (get-recursion-interaction sender receiver label branch)
                             :else (log-error :unsupported-operation (format "Cannot check operation on child branchable construct! %s" (interaction-to-string branch)))))))))

(defn remove-from-parallel
  "Remove an interaction from a parallel in a recursive fashion."
  [sender receivers label target-interaction monitor]
  (let [pars (flatten (filter some?
                              (for [par (get-parallel target-interaction)]
                                (let [inter (cond
                                              (satisfies? parallelizable par)
                                              (remove-from-parallel sender receivers label par monitor)
                                              (satisfies? interactable par)
                                              (get-atomic-interaction sender receivers label par)
                                              (satisfies? branchable par)
                                              (get-branch-interaction sender receivers label par)
                                              (satisfies? recursable par)
                                              (get-recursion-interaction sender receivers label par)
                                              (satisfies? identifiable-recur par)
                                              (let [recursion (get-rec monitor (get-name par))
                                                    valid-rec (get-recursion-interaction sender receivers label recursion)]
                                                (if (nil? valid-rec)
                                                  par
                                                  recursion))
                                              :else
                                              par)]
                                  (if (nil? inter)
                                    (if (satisfies? parallelizable par)
                                      nil
                                      par)
                                    (cond
                                      (satisfies? parallelizable inter)
                                      (remove-from-parallel sender receivers label inter monitor)
                                      (satisfies? interactable inter)
                                      (if (multiple-receivers? inter)
                                        (assoc inter :receivers (vec (remove #{receivers} (:receivers inter))))
                                        (get-next inter))
                                      (or (satisfies? recursable inter) (satisfies? branchable inter))
                                      inter
                                      (and (instance? clojure.lang.LazySeq inter) (not (satisfies? interactable inter)))
                                      (first (filter some? inter))))))))]
    (if (empty? pars)
      (get-next target-interaction)
      (assoc target-interaction :parallels pars))))

(defn- swap-active-interaction-by-parallel
  "Swap active interaction by parallel"
  [sender receivers label active-interaction target-interaction monitor]
  (let [target-parallel-interaction (get-parallel-interaction sender receivers label target-interaction)]
    (swap! active-interaction
           (fn [inter]
             (let [target (if (= (get-id inter) (get-id target-parallel-interaction))
                            (get-parallel-interaction sender receivers label inter)
                            target-parallel-interaction)]
               (if (nil? target)
                 inter
                 (remove-from-parallel sender receivers label target monitor))))))
  true)


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
    (satisfies? closable active-interaction)
    (is-valid-close? sender receivers active-interaction)
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
     (apply-receive-to-mon monitor sender receivers label active-interaction (get-branch-interaction sender receivers label target-interaction))
     (satisfies? parallelizable target-interaction)
     (swap-active-interaction-by-parallel sender receivers label active-interaction target-interaction monitor)
     (satisfies? recursable target-interaction)
     (apply-receive-to-mon monitor sender receivers label active-interaction (get-recursion target-interaction))
     (satisfies? identifiable-recur target-interaction)
     (apply-receive-to-mon monitor sender receivers label active-interaction (get-rec monitor (get-name target-interaction)))
     :else (log-error :unsupported-operation (format "Unsupported type of interaction to apply %s!" (type target-interaction)))
     )))