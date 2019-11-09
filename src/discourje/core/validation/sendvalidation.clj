;;sendvalidation.clj
;(in-ns 'discourje.core.async)
;
;;forward declare
;(declare get-send-branch-interaction get-send-parallel-interaction is-valid-interaction? interaction-to-string is-valid-communication? interaction-to-string is-active-interaction-multicast? add-rec-to-table)
;
;(defn- assoc-sender-to-interaction
;  "Assoc a sender to the accepted-sends set and return in"
;  ([inter sender is-found]
;   (reset! is-found true)
;   (assoc-sender-to-interaction inter sender))
;  ([inter sender]
;   (assoc inter :accepted-sends (conj (:accepted-sends inter) sender))))
;
;(defn- send-active-interaction-by-atomic
;  "Send active interaction by atomic"
;  [active-interaction pre-swap-interaction target-interaction sender]
;  (if (nil? sender)
;    (log-error :invalid-send (format "sender appears to be nil: %s %s" active-interaction target-interaction))
;    (= (get-id (swap! active-interaction (fn [inter]
;                                           (if (= (get-id inter) (get-id pre-swap-interaction))
;                                             (assoc-sender-to-interaction target-interaction sender)
;                                             inter)
;                                           )))
;       (get-id target-interaction))))
;
;(defn- is-valid-interaction-for-send?
;  "Check if the interaction is valid for a send operation"
;  [sender receivers message active-interaction]
;  (when (and (is-valid-interaction? sender receivers message active-interaction)
;             (false? (contains? (get-accepted-sends active-interaction) sender)))
;    active-interaction))
;
;(defn- get-send-atomic-interaction
;  "get the atomic interaction when it is valid for send"
;  [sender receiver message active-interaction]
;  (when (is-valid-interaction-for-send? sender receiver message active-interaction) active-interaction))
;
;(defn- get-send-recur-identifier-interaction
;  "Check the first element in a recursion interaction"
;  [monitor sender receiver message rec]
;  (cond
;    (satisfies? interactable rec) (get-send-atomic-interaction sender receiver message rec)
;    (satisfies? branchable rec) (get-send-branch-interaction monitor sender receiver message rec)
;    (satisfies? parallelizable rec) (get-send-parallel-interaction monitor sender receiver message rec)
;    (satisfies? closable rec) nil
;    (satisfies? identifiable-recur rec) (get-send-recur-identifier-interaction monitor sender receiver message (get-rec monitor (get-name rec)))
;    :else (log-error :unsupported-operation (format "No correct next recursion monitor found. %s" (interaction-to-string rec)))))
;
;(defn- get-send-parallel-interaction
;  "Check the atomic interaction"
;  [monitor sender receiver message active-interaction]
;  (when-let [_ (first (filter #(cond
;                                 (satisfies? interactable %) (get-send-atomic-interaction sender receiver message %)
;                                 (satisfies? branchable %) (get-send-branch-interaction monitor sender receiver message %)
;                                 (satisfies? parallelizable %) (get-send-parallel-interaction monitor sender receiver message %)
;                                 (satisfies? closable %) nil
;                                 (satisfies? identifiable-recur %) (get-send-recur-identifier-interaction monitor sender receiver message (get-rec monitor (get-name %)))
;                                 :else (log-error :unsupported-operation (format "Cannot check operation on child parallel construct! %s" (interaction-to-string %))))
;                              (get-parallel active-interaction))
;                      )]
;    active-interaction))
;
;(defn- get-send-branch-interaction
;  "Check the atomic interaction"
;  [monitor sender receiver message active-interaction]
;  (first (filter #(cond
;                    (satisfies? interactable %) (get-send-atomic-interaction sender receiver message %)
;                    (satisfies? branchable %) (get-send-branch-interaction monitor sender receiver message %)
;                    (satisfies? parallelizable %) (get-send-parallel-interaction monitor sender receiver message %)
;                    (satisfies? closable %) nil
;                    (satisfies? identifiable-recur %) (get-send-recur-identifier-interaction monitor sender receiver message (get-rec monitor (get-name %)))
;                    :else (log-error :unsupported-operation (format "Cannot check operation on child branchable construct! %s" (interaction-to-string %))))
;                 (get-branches active-interaction))))
;
;(defn- set-send-on-par
;  "Assoc a sender to a nested parallel interaction"
;  [sender receivers message target-interaction monitor]
;  (let [pars (let [pars (get-parallel target-interaction)
;                   is-found (atom false)]
;               (for [p pars]
;                 (cond
;                   @is-found p
;                   (satisfies? interactable p)
;                   (if (is-valid-interaction-for-send? sender receivers message p)
;                     (assoc-sender-to-interaction p sender is-found)
;                     p)
;                   (satisfies? branchable p)
;                   (let [valid-branch (get-send-branch-interaction monitor sender receivers message p)]
;                     (if (not (nil? valid-branch))
;                       (if (satisfies? parallelizable valid-branch)
;                         (set-send-on-par sender receivers message valid-branch monitor)
;                         (assoc-sender-to-interaction valid-branch sender is-found))
;                       p))
;                   (satisfies? parallelizable p)
;                   (set-send-on-par sender receivers message p monitor)
;                   (satisfies? identifiable-recur p)
;                   (let [valid-rec (get-send-recur-identifier-interaction monitor sender receivers message (get-rec monitor (get-name p)))]
;                     (if (not (nil? valid-rec))
;                       (if (satisfies? parallelizable valid-rec)
;                         (set-send-on-par sender receivers message valid-rec monitor)
;                         (assoc-sender-to-interaction valid-rec sender is-found))
;                       p))
;                   (satisfies? closable p)
;                   p
;                   )))]
;    (let [duplicate-par (first (filter some? (filter (fn [p] (= (get-id p) (get-id target-interaction))) pars)))]
;      (if (nil? duplicate-par)
;        (assoc target-interaction :parallels pars)
;        duplicate-par))))
;
;(defn- send-active-interaction-by-parallel
;  "Swap active interaction by parallel"
;  [sender receivers message active-interaction target-interaction monitor]
;  (swap! active-interaction
;         (fn [inter]
;           (if (= (get-id inter) (get-id target-interaction))
;             (set-send-on-par sender receivers message inter monitor)
;             (set-send-on-par sender receivers message target-interaction monitor))))
;  true)
;
;(defn- apply-send-to-mon
;  "Apply new interaction"
;  ([monitor sender receivers message active-interaction pre-swap-interaction target-interaction]
;   (cond
;     (satisfies? interactable target-interaction)
;     (send-active-interaction-by-atomic active-interaction pre-swap-interaction target-interaction sender)
;     (satisfies? branchable target-interaction)
;     (apply-send-to-mon monitor sender receivers message active-interaction pre-swap-interaction (get-send-branch-interaction monitor sender receivers message target-interaction))
;     (satisfies? parallelizable target-interaction)
;     (send-active-interaction-by-parallel sender receivers message active-interaction target-interaction monitor)
;     (satisfies? identifiable-recur target-interaction)
;     (apply-send-to-mon monitor sender receivers message active-interaction pre-swap-interaction (get-rec monitor (get-name target-interaction)))
;     :else (log-error :unsupported-operation (format "Unsupported type of interaction to apply %s!" (type target-interaction)))
;     )))
;
;(defn is-valid-send-communication?
;  "Checks if communication is valid by comparing input to the active monitor"
;  [monitor sender receivers message active-interaction]
;  (cond
;    (satisfies? interactable active-interaction)
;    (is-valid-interaction-for-send? sender receivers message active-interaction)
;    (satisfies? branchable active-interaction)
;    (first (filter #(is-valid-send-communication? monitor sender receivers message %) (get-branches active-interaction)))
;    (satisfies? parallelizable active-interaction)
;    (get-send-parallel-interaction monitor sender receivers message active-interaction)
;    (satisfies? identifiable-recur active-interaction)
;    (if (satisfies? parallelizable active-interaction)
;      (when (<= (count (get-parallel active-interaction)) 1)
;        (is-valid-send-communication? monitor sender receivers message (get-rec monitor (get-name active-interaction))))
;      (is-valid-send-communication? monitor sender receivers message (get-rec monitor (get-name active-interaction))))
;    (satisfies? closable active-interaction)
;    nil
;    :else
;    (log-error :unsupported-operation (format "Unsupported communication type: Communication invalid, type: %s" (type active-interaction)))))