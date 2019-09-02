;sendvalidation.clj
(in-ns 'discourje.core.async)

;forward declare check-branchable-interaction to resolve undefined issue in check-recursion-interaction
(declare get-send-branch-interaction get-valid-send-branch-interaction get-send-parallel-interaction is-valid-interaction? interaction-to-string is-valid-communication? interaction-to-string is-active-interaction-multicast? add-rec-to-table)

(defn- assoc-sender-to-interaction
  "Assoc a sender to the accepted-sends set and return in"
  [inter sender]
  (assoc inter :accepted-sends (conj (:accepted-sends inter) sender)))

(defn- send-active-interaction-by-atomic
  "Send active interaction by atomic"
  [active-interaction target-interaction sender]
  (let [active-interaction-before-swap @active-interaction]
    (if (nil? sender)
      (log-error :invalid-send (format "sender appears to be nil: %s %s" active-interaction target-interaction))
      (= (get-id (swap! active-interaction (fn [inter]
                                  (if (= (get-id inter) (get-id active-interaction-before-swap))
                                    (assoc-sender-to-interaction inter sender)
                                    inter))))
         (if (nil? target-interaction)
           "discourje"
           (get-id target-interaction))))))

(defn- is-valid-interaction-for-send?
  "Check if the interaction is valid for a send operation"
  [sender receivers label active-interaction]
  (and (is-valid-interaction? sender receivers label active-interaction) (false? (contains? (get-accepted-sends active-interaction) sender))))

(defn- get-send-atomic-interaction
  "get the atomic interaction when it is valid for send"
  [sender receiver label active-interaction]
  (when (is-valid-interaction-for-send? sender receiver label active-interaction) active-interaction))

(defn- get-send-recursion-interaction
  "Check the first element in a recursion interaction"
  [sender receiver label active-interaction]
  (let [rec (get-recursion active-interaction)]
    (cond
      (satisfies? interactable rec) (get-send-atomic-interaction sender receiver label rec)
      (satisfies? branchable rec) (get-valid-send-branch-interaction sender receiver label rec)
      (satisfies? recursable rec) (get-send-recursion-interaction sender receiver label rec)
      (satisfies? parallelizable rec) (get-send-parallel-interaction sender receiver label rec)
      :else (log-error :unsupported-operation (format "No correct next recursion monitor found. %s" (interaction-to-string rec))))))

(defn- get-send-parallel-interaction
  "Check the atomic interaction"
  [sender receiver label active-interaction]
  (when-let [_ (for [parallel (get-parallel active-interaction)]
                 (cond
                   (satisfies? interactable parallel) (get-send-atomic-interaction sender receiver label parallel)
                   (satisfies? branchable parallel) (get-valid-send-branch-interaction sender receiver label parallel)
                   (satisfies? parallelizable parallel) (get-send-parallel-interaction sender receiver label parallel)
                   (satisfies? recursable parallel) (get-send-recursion-interaction sender receiver label parallel)
                   :else (log-error :unsupported-operation (format "Cannot check operation on child parallel construct! %s" (interaction-to-string parallel))))
                 )]
    active-interaction))

(defn- get-all-send-branch-interaction
  "Check the atomic interaction"
  [sender receiver label active-interaction]
  (flatten
    (for [branch (:branches active-interaction)]
      (cond
        (satisfies? interactable branch) (get-send-atomic-interaction sender receiver label branch)
        (satisfies? branchable branch) (get-valid-send-branch-interaction sender receiver label branch)
        (satisfies? parallelizable branch) (get-send-parallel-interaction sender receiver label branch)
        (satisfies? recursable branch) (get-send-recursion-interaction sender receiver label branch)
        :else (log-error :unsupported-operation (format "Cannot check operation on child branchable construct! %s" (interaction-to-string branch)))))))

(defn- get-valid-send-branch-interaction
  "Find the first interactable in (nested) branchable constructs."
  [sender receiver label active-interaction]
  (first (filter some? (get-all-send-branch-interaction sender receiver label active-interaction))))

(defn- set-send-on-par
  "Assoc a sender to a nested parallel interaction"
  [sender receivers label inter target-interaction monitor]
  (let [pars (let [pars (get-parallel target-interaction)]
               (for [p pars]
                 (cond
                   (satisfies? interactable p)
                   (if (is-valid-interaction-for-send? sender receivers label p)
                     (assoc-sender-to-interaction p sender)
                     p)
                   (satisfies? branchable p)
                   (let [valid-branch (get-valid-send-branch-interaction sender receivers label p)]
                     (if (not (nil? valid-branch))
                       (if (satisfies? parallelizable valid-branch)
                         (set-send-on-par sender receivers label target-interaction valid-branch monitor)
                         (assoc-sender-to-interaction valid-branch sender))
                       p))
                   (satisfies? parallelizable p)
                   (set-send-on-par sender receivers label target-interaction p monitor)
                   (satisfies? recursable p)
                   (let [valid-rec (get-send-recursion-interaction sender receivers label p)]
                     (if (not (nil? valid-rec))
                       (if (satisfies? parallelizable valid-rec)
                         (set-send-on-par sender receivers label target-interaction valid-rec monitor)
                         (assoc-sender-to-interaction valid-rec sender))
                       p))
                   (satisfies? identifiable-recur p)
                   (let [valid-rec (get-send-recursion-interaction sender receivers label (get-rec monitor (get-name p)))]
                     (if (not (nil? valid-rec))
                       (if (satisfies? parallelizable valid-rec)
                         (set-send-on-par sender receivers label target-interaction valid-rec monitor)
                         (assoc-sender-to-interaction valid-rec sender))
                       p))
                   )))]
    (let [duplicate-par (first (filter some? (filter (fn [p] (= (get-id p) (get-id target-interaction))) pars)))]
      (if (nil? duplicate-par)
        (assoc target-interaction :parallels pars)
        duplicate-par))))

(defn- send-active-interaction-by-parallel
  "Swap active interaction by parallel"
  [sender receivers label active-interaction target-interaction monitor]
  (swap! active-interaction
         (fn [inter]
           (if (= (get-id inter) (get-id target-interaction))
             (set-send-on-par sender receivers label inter inter monitor)
             (set-send-on-par sender receivers label inter target-interaction monitor))))
  true)

(defn- apply-send-to-mon
  "Apply new interaction"
  ([monitor sender receivers label active-interaction target-interaction]
   (log-message (format "Applying: SEND label %s, receiver %s." label receivers))
   (cond
     (satisfies? interactable target-interaction)
     (send-active-interaction-by-atomic active-interaction target-interaction sender)
     (satisfies? branchable target-interaction)
     (apply-send-to-mon monitor sender receivers label active-interaction (get-valid-send-branch-interaction sender receivers label target-interaction))
     (satisfies? parallelizable target-interaction)
     (send-active-interaction-by-parallel sender receivers label active-interaction target-interaction monitor)
     (satisfies? recursable target-interaction)
     (apply-send-to-mon monitor sender receivers label active-interaction (get-recursion target-interaction))
     (satisfies? identifiable-recur target-interaction)
     (apply-send-to-mon monitor sender receivers label active-interaction (get-rec monitor (get-name target-interaction)))
     :else (log-error :unsupported-operation (format "Unsupported type of interaction to apply %s!" (type target-interaction)))
     )))

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
    (and (is-valid-send-communication? monitor sender receivers label (get-rec monitor (get-name active-interaction)))
         (if (satisfies? parallelizable active-interaction)
           (<= (count (get-parallel active-interaction)) 1)
           true))
    :else
    (do (log-error :unsupported-operation (format "Unsupported communication type: Communication invalid, type: %s" (type active-interaction)))
        false)))