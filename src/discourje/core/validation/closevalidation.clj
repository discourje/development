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
  (cond
    (satisfies? closable active-interaction)
    (when (is-valid-close? sender receivers active-interaction)
      active-interaction)
    (satisfies? interactable active-interaction)
    nil
    (satisfies? branchable active-interaction)
    (first (filter #(is-valid-close-communication? monitor sender receivers %) (get-branches active-interaction)))
    (satisfies? parallelizable active-interaction)
    (first (filter #(is-valid-close-communication? monitor sender receivers %) (get-parallel active-interaction)))
    (satisfies? identifiable-recur active-interaction)
    (if (satisfies? parallelizable active-interaction)
      (when (<= (count (get-parallel active-interaction)) 1)
        (is-valid-close-communication? monitor sender receivers (get-rec monitor (get-name active-interaction))))
      (is-valid-close-communication? monitor sender receivers (get-rec monitor (get-name active-interaction))))
    :else
    (log-error :unsupported-operation (format "Unsupported communication type: Communication invalid, type: %s" (type active-interaction)))))

(defn- swap-active-interaction-by-close
  "Apply new interaction"
  [active-interaction pre-swap-interaction target-interaction]
  (swap-active-interaction-by-atomic active-interaction pre-swap-interaction target-interaction nil))
;------------------------------------------------------------------------------------
(defn- get-close-recur-Identifier-interaction
  "Check the first element in a recursion interaction"
  [monitor sender receiver rec]
  (cond
    (satisfies? closable rec) (when (is-valid-close? sender receiver rec) rec)
    (satisfies? interactable rec) nil
    (satisfies? branchable rec) (get-close-branch-interaction monitor sender receiver rec)
    (satisfies? parallelizable rec) (get-close-parallel-interaction monitor sender receiver rec)
    :else (log-error :unsupported-operation (format "No correct next recursion monitor found. %s" (interaction-to-string rec)))))

(defn- get-close-parallel-interaction
  "Check the atomic interaction"
  [monitor sender receiver active-interaction]
  (when-let [_ (first (filter
                        #(cond
                           (satisfies? closable %) (when (is-valid-close? sender receiver %) %)
                           (satisfies? interactable %) nil
                           (satisfies? branchable %) (get-close-branch-interaction monitor sender receiver %)
                           (satisfies? parallelizable %) (get-close-parallel-interaction monitor sender receiver %)
                           (satisfies? identifiable-recur %) (get-close-recur-Identifier-interaction monitor sender receiver (get-rec monitor (get-name %)))
                           :else (log-error :unsupported-operation (format "Cannot check operation on child parallel construct! %s" (interaction-to-string %))))
                        (get-parallel active-interaction)))]
    active-interaction))

(defn- get-close-branch-interaction
  "Check the atomic interaction"
  [monitor sender receiver active-interaction]
  (first (filter #(cond
                    (satisfies? closable %) (when (is-valid-close? sender receiver %) %)
                    (satisfies? interactable %) nil
                    (satisfies? branchable %) (get-close-branch-interaction monitor sender receiver %)
                    (satisfies? parallelizable %) (get-close-parallel-interaction monitor sender receiver %)
                    (satisfies? identifiable-recur %) (get-close-recur-Identifier-interaction monitor sender receiver (get-rec monitor (get-name %)))
                    :else (log-error :unsupported-operation (format "Cannot check operation on child branchable construct! %s" (interaction-to-string %))))
                 (get-branches active-interaction))))

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
                                              (get-close-branch-interaction monitor sender receivers par)
                                              (satisfies? identifiable-recur par)
                                              (let [recursion (get-rec monitor (get-name par))
                                                    valid-rec (get-close-recur-Identifier-interaction monitor sender receivers recursion)]
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
                                      (or (satisfies? branchable inter) (satisfies? interactable inter))
                                      inter
                                      (and (instance? clojure.lang.LazySeq inter) (not (satisfies? interactable inter)))
                                      (first (filter some? inter))))))))]
    (if (empty? pars)
      (get-next target-interaction)
      (assoc target-interaction :parallels pars))))

(defn- swap-active-close-interaction-by-parallel
  "Swap active interaction by parallel"
  [sender receivers active-interaction target-interaction monitor]
  (let [target-parallel-interaction (get-close-parallel-interaction monitor sender receivers target-interaction)]
    (swap! active-interaction
           (fn [inter]
             (let [target (if (= (get-id inter) (get-id target-parallel-interaction))
                            (get-close-parallel-interaction monitor sender receivers inter)
                            target-parallel-interaction)]
               (if (nil? target)
                 inter
                 (remove-close-from-parallel sender receivers target monitor))))))
  true)

(defn- apply-close-to-mon
  "Apply new interaction"
  ([monitor channel active-interaction pre-swap-interaction target-interaction]
   (if
     (cond
       (satisfies? closable target-interaction)
       (swap-active-interaction-by-close active-interaction pre-swap-interaction target-interaction)
       (satisfies? interactable target-interaction)
       false
       (satisfies? branchable target-interaction)
       (apply-close-to-mon monitor channel active-interaction pre-swap-interaction (get-close-branch-interaction monitor (get-provider channel) (get-consumer channel) target-interaction))
       (satisfies? parallelizable target-interaction)
       (swap-active-close-interaction-by-parallel (get-provider channel) (get-consumer channel) active-interaction target-interaction monitor)
       (satisfies? identifiable-recur target-interaction)
       (apply-close-to-mon monitor channel active-interaction pre-swap-interaction (get-rec monitor (get-name target-interaction)))
       :else (do (log-error :unsupported-operation (format "Unsupported type of interaction to apply %s!" (type target-interaction)))
                 false))
     (do (clojure.core.async/close! (get-chan channel))
         true)
     false
     )))