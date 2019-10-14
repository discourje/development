;receivevalidation.clj
(in-ns 'discourje.core.async)

;forward declare
(declare get-branch-interaction get-parallel-interaction is-valid-interaction? interaction-to-string is-active-interaction-multicast? add-rec-to-table)

(defn- multiple-receivers?
  "Does the monitor have multiple receivers?"
  [active-interaction]
  (and (instance? Seqable (:receivers active-interaction)) (> (count (:receivers active-interaction)) 1)))

(defn- remove-receiver
  "Remove a receiver from the active monitor"
  [active-interaction current-interaction receiver]
  (if (satisfies? interactable current-interaction)
    (swap! active-interaction (fn [inter]
                                (if (= (get-id inter) (get-id current-interaction))
                                  (if (multiple-receivers? inter)
                                    (assoc inter :receivers (vec (remove #{receiver} (:receivers inter))))
                                    (get-next current-interaction))
                                  (assoc current-interaction :receivers (vec (remove #{receiver} (:receivers current-interaction)))))))
    (log-error :unsupported-operation (format "Cannot remove-receiver from interaction of type: %s, it should be atomic! Interaction = %s" (type current-interaction) (interaction-to-string current-interaction)))))

(defn- swap-active-interaction-by-atomic
  "Swap active interaction by atomic
  The end-protocol comparison indicates the protocol is terminated."
  [active-interaction pre-swap-interaction target-interaction receiver]
  (if (multiple-receivers? target-interaction)
    (remove-receiver active-interaction target-interaction receiver)
    (let [swapped (swap! active-interaction (fn [inter]
                                              (if (= (get-id inter) (get-id pre-swap-interaction))
                                                (get-next target-interaction)
                                                inter)))]
      (= (if (nil? swapped) "end-protocol" (get-id swapped))
         (if (or (nil? target-interaction) (nil? (get-next target-interaction)))
           "end-protocol"
           (get-id (get-next target-interaction)))))))


(defn- get-atomic-interaction
  "Check the atomic interaction"
  [sender receiver label active-interaction]
  (when (is-valid-interaction? sender receiver label active-interaction) active-interaction))

(defn- get-recur-identifier-interaction
  "Check the first element in a recursion interaction"
  [sender receiver label rec]
  (cond
    (satisfies? interactable rec) (get-atomic-interaction sender receiver label rec)
    (satisfies? branchable rec) (get-branch-interaction sender receiver label rec)
    (satisfies? recursable rec) (get-recur-identifier-interaction sender receiver label rec)
    (satisfies? parallelizable rec) (get-parallel-interaction sender receiver label rec)
    (satisfies? closable rec) nil
    :else (log-error :unsupported-operation (format "No correct next recursion monitor found. %s" (interaction-to-string rec)))))

(defn- get-parallel-interaction
  "Check the atomic interaction"
  [sender receiver label active-interaction]
  (when-let [_ (first (filter
                        #(cond
                           (satisfies? interactable %) (get-atomic-interaction sender receiver label %)
                           (satisfies? branchable %) (get-branch-interaction sender receiver label %)
                           (satisfies? parallelizable %) (get-parallel-interaction sender receiver label %)
                           (satisfies? closable %) nil
                           :else (log-error :unsupported-operation (format "Cannot check operation on child parallel construct! %s" (interaction-to-string %))))
                        (get-parallel active-interaction))
                      )]
    active-interaction))

(defn- get-branch-interaction
  "Check the atomic interaction"
  [sender receiver label active-interaction]
  (first (filter #(cond
                    (satisfies? interactable %) (get-atomic-interaction sender receiver label %)
                    (satisfies? branchable %) (get-branch-interaction sender receiver label %)
                    (satisfies? parallelizable %) (get-parallel-interaction sender receiver label %)
                    (satisfies? closable %) nil
                    :else (log-error :unsupported-operation (format "Cannot check operation on child branchable construct! %s" (interaction-to-string %))))
                 (get-branches active-interaction))))

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
                                              (satisfies? identifiable-recur par)
                                              (let [recursion (get-rec monitor (get-name par))
                                                    valid-rec (get-recur-identifier-interaction sender receivers label recursion)]
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
                                      (or (satisfies? branchable inter) (satisfies? closable inter))
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
    (get-atomic-interaction sender receivers label active-interaction)
    (satisfies? branchable active-interaction)
    (first (filter #(is-valid-communication? monitor sender receivers label %) (get-branches active-interaction)))
    (satisfies? parallelizable active-interaction)
    (get-parallel-interaction sender receivers label active-interaction)
    (satisfies? identifiable-recur active-interaction)
    (is-valid-communication? monitor sender receivers label (get-rec monitor (get-name active-interaction)))
    (satisfies? closable active-interaction)
    nil
    :else
    (log-error :unsupported-operation (format "Unsupported communication type: Communication invalid, type: %s" (type active-interaction)))))

(defn- apply-receive-to-mon
  "Apply new interaction"
  ([monitor sender receivers label active-interaction pre-swap-interaction target-interaction]
   (cond
     (satisfies? interactable target-interaction)
     (swap-active-interaction-by-atomic active-interaction pre-swap-interaction target-interaction receivers)
     (satisfies? branchable target-interaction)
     (apply-receive-to-mon monitor sender receivers label active-interaction pre-swap-interaction (get-branch-interaction sender receivers label target-interaction))
     (satisfies? parallelizable target-interaction)
     (swap-active-interaction-by-parallel sender receivers label active-interaction target-interaction monitor)
     (satisfies? identifiable-recur target-interaction)
     (apply-receive-to-mon monitor sender receivers label active-interaction pre-swap-interaction (get-rec monitor (get-name target-interaction)))
     :else (log-error :unsupported-operation (format "Unsupported type of interaction to apply %s!" (type target-interaction)))
     )))