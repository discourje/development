;closevalidation.clj
(in-ns 'discourje.core.async)
(declare swap-active-interaction-by-atomic get-close-branch-interaction get-close-parallel-interaction)
(defn is-valid-close?
  "Is the active interaction a valid close?"
  [sender receivers active-interaction]
  (and
    (= sender (get-from active-interaction))
    (= receivers (get-to active-interaction))))

(defn is-valid-close-communication?
  "Checks if communication is valid by comparing input to the active monitor"
  [monitor sender receivers active-interaction]
  (println (format "is valid close? %s %s interaction %s" sender receivers (interaction-to-string active-interaction)))
  (cond
    (satisfies? closable active-interaction)
    (is-valid-close? sender receivers active-interaction)
    (satisfies? interactable active-interaction)
    false
    (satisfies? branchable active-interaction)
    (> (count (filter true? (flatten (for [b (:branches active-interaction)] (is-valid-close-communication? monitor sender receivers b))))) 0)
    (satisfies? parallelizable active-interaction)
    (> (count (filter true? (flatten (for [p (get-parallel active-interaction)] (is-valid-close-communication? monitor sender receivers p))))) 0)
    (satisfies? recursable active-interaction)
    (do (register-rec! monitor active-interaction)
        (is-valid-close-communication? monitor sender receivers (get-recursion active-interaction)))
    (satisfies? identifiable-recur active-interaction)
    (and (is-valid-close-communication? monitor sender receivers (get-rec monitor (get-name active-interaction)))
         (if (satisfies? parallelizable active-interaction)
           (<= (count (get-parallel active-interaction)) 1)
           true))
    :else
    (do (log-error :unsupported-operation (format "Unsupported communication type: Communication invalid, type: %s" (type active-interaction)))
        false)))

(defn- swap-active-interaction-by-close
  "Apply new interaction"
  [channel active-interaction target-interaction]
  (log-message (format "Applying: Close sender %s, receiver %s." (get-provider channel) (get-consumer channel)))
  (swap-active-interaction-by-atomic active-interaction target-interaction nil))
;------------------------------------------------------------------------------------
(defn- get-close-recursion-interaction
  "Check the first element in a recursion interaction"
  [sender receiver active-interaction]
  (let [rec (get-recursion active-interaction)]
    (cond
      (satisfies? closable rec) (when (is-valid-close? sender receiver rec) rec)
      (satisfies? interactable rec) nil
      (satisfies? branchable rec) (get-close-branch-interaction sender receiver rec)
      (satisfies? recursable rec) (get-close-recursion-interaction sender receiver rec)
      (satisfies? parallelizable rec) (get-close-parallel-interaction sender receiver rec)
      :else (log-error :unsupported-operation (format "No correct next recursion monitor found. %s" (interaction-to-string rec))))))

(defn- get-close-parallel-interaction
  "Check the atomic interaction"
  [sender receiver active-interaction]
  (when-let [_ (for [parallel (get-parallel active-interaction)]
                 (cond
                   (satisfies? closable parallel) (when (is-valid-close? sender receiver parallel) parallel)
                   (satisfies? interactable parallel) nil
                   (satisfies? branchable parallel) (get-close-branch-interaction sender receiver parallel)
                   (satisfies? parallelizable parallel) (get-close-parallel-interaction sender receiver parallel)
                   (satisfies? recursable parallel) (get-close-recursion-interaction sender receiver parallel)
                   :else (log-error :unsupported-operation (format "Cannot check operation on child parallel construct! %s" (interaction-to-string parallel))))
                 )]
    active-interaction))
(defn- get-close-branch-interaction
  "Check the atomic interaction"
  [sender receiver active-interaction]
  (first (filter some? (flatten
                         (for [branch (:branches active-interaction)]
                           (cond
                             (satisfies? closable branch) (when (is-valid-close? sender receiver branch) branch)
                             (satisfies? interactable branch) nil
                             (satisfies? branchable branch) (get-close-branch-interaction sender receiver branch)
                             (satisfies? parallelizable branch) (get-close-parallel-interaction sender receiver branch)
                             (satisfies? recursable branch) (get-close-recursion-interaction sender receiver branch)
                             :else (log-error :unsupported-operation (format "Cannot check operation on child branchable construct! %s" (interaction-to-string branch)))))))))

(defn remove-close-from-parallel
  "Remove an interaction from a parallel in a recursive fashion."
  [sender receivers target-interaction monitor]
  (let [pars (flatten (filter some?
                              (for [par (get-parallel target-interaction)]
                                (let [inter (cond
                                              (satisfies? parallelizable par)
                                              (remove-close-from-parallel sender receivers par monitor)
                                              (satisfies? closable par)
                                              (when (is-valid-close? sender receivers par) par)
                                              (satisfies? branchable par)
                                              (get-close-branch-interaction sender receivers par)
                                              (satisfies? recursable par)
                                              (get-close-recursion-interaction sender receivers par)
                                              (satisfies? identifiable-recur par)
                                              (let [recursion (get-rec monitor (get-name par))
                                                    valid-rec (get-close-recursion-interaction sender receivers recursion)]
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
                                      (remove-close-from-parallel sender receivers inter monitor)
                                      (satisfies? closable inter)
                                      (get-next inter)
                                      (or (satisfies? recursable inter) (satisfies? branchable inter) (satisfies? interactable inter))
                                      inter
                                      (and (instance? clojure.lang.LazySeq inter) (not (satisfies? interactable inter)))
                                      (first (filter some? inter))))))))]
    (if (empty? pars)
      (get-next target-interaction)
      (assoc target-interaction :parallels pars))))

(defn- swap-active-close-interaction-by-parallel
  "Swap active interaction by parallel"
  [sender receivers active-interaction target-interaction monitor]
  (let [target-parallel-interaction (get-close-parallel-interaction sender receivers target-interaction)]
    (swap! active-interaction
           (fn [inter]
             (let [target (if (= (get-id inter) (get-id target-parallel-interaction))
                            (get-close-parallel-interaction sender receivers inter)
                            target-parallel-interaction)]
               (if (nil? target)
                 inter
                 (remove-close-from-parallel sender receivers target monitor))))))
  true)

(defn- apply-close-to-mon
  "Apply new interaction"
  ([monitor channel active-interaction target-interaction]
   (log-message (format "Applying: CLOSE %s, receiver %s." (get-provider channel) (get-consumer channel)))
   (if
     (cond
       (satisfies? closable target-interaction)
       (swap-active-interaction-by-close channel active-interaction target-interaction)
       (satisfies? interactable target-interaction)
       false
       (satisfies? branchable target-interaction)
       (apply-close-to-mon monitor channel active-interaction (get-close-branch-interaction (get-provider channel) (get-consumer channel) target-interaction))
       (satisfies? parallelizable target-interaction)
       (swap-active-close-interaction-by-parallel (get-provider channel) (get-consumer channel) active-interaction target-interaction monitor)
       (satisfies? recursable target-interaction)
       (apply-close-to-mon monitor channel active-interaction (get-recursion target-interaction))
       (satisfies? identifiable-recur target-interaction)
       (apply-close-to-mon monitor channel active-interaction (get-rec monitor (get-name target-interaction)))
       :else (do (log-error :unsupported-operation (format "Unsupported type of interaction to apply %s!" (type target-interaction)))
                 false))
     (do (clojure.core.async/close! (get-chan channel))
         true)
     false
     )))