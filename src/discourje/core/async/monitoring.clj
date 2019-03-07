;monitoring.clj
(in-ns 'discourje.core.async.async)

;forward declare check-branch-interaction to resolve undefined issue in check-recursion-interaction
(declare check-branch-interaction get-branch-interaction)

(defprotocol monitoring
  (get-monitor-id [this])
  (get-active-interaction [this])
  ;(send-interaction [this label])
  (apply-interaction [this sender receivers label])
  (valid-interaction? [this sender receivers label]))

(defn- check-atomic-interaction
  "Check the atomic interaction"
  [label active-interaction]
  (= (get-action active-interaction) label))

(defn- check-recursion-interaction
  "Check the first element in a recursion interaction"
  [label active-interaction]
  (let [rec (get-recursion active-interaction)
        first-interaction (first rec)]
    (cond
      (satisfies? interactable first-interaction) (check-atomic-interaction label first-interaction)
      (satisfies? branch first-interaction) (check-branch-interaction label first-interaction)
      (satisfies? recursable first-interaction) (check-recursion-interaction label first-interaction)
      :else (log-error :unsupported-operation "No correct next recursion monitor found"))))

(defn- check-branch-interaction
  "Check the atomic interaction"
  [label active-interaction]
  (> (count (filter (fn [x] (true? x))
                    (flatten
                      (for [b (:branches active-interaction)]
                        (let [first-in-branch (nth b 0)]
                          (cond
                            (satisfies? interactable first-in-branch) (check-atomic-interaction label first-in-branch)
                            (satisfies? branch first-in-branch) (check-branch-interaction label first-in-branch)
                            (satisfies? recursable first-in-branch) (check-recursion-interaction label first-in-branch)
                            :else (log-error :unsupported-operation "Cannot check operation on child branch construct!")))))))
     0))

(defn- swap-next-interaction!
  "Get the next interaction"
  [interactions]
  (fn [active-interaction]
    (first (filter
             (fn [inter]
               (cond (satisfies? interactable active-interaction) (= (get-id inter) (get-next active-interaction))
                     :else (do (log-error :unsupported-operation "Not supported type!") false)))
             interactions))))


(defn- find-nested-next
  "Finds the next interaction based on id, nested in choices"
  [id interactions]
  (first (flatten (filter some? (for [inter interactions]
                                  (cond (satisfies? interactable inter) (when (= (get-id inter) id) inter)
                                        (satisfies? branch inter) (if (= id (get-id inter))
                                                                    inter
                                                                    (let [branches (:branches inter)
                                                                          searches (for [b branches] (find-nested-next id b))]
                                                                      (first (filter some? searches))))
                                        (satisfies? recursable inter) (if (= id (get-id inter))
                                                                        inter
                                                                        (find-nested-next id (get-recursion inter)))
                                        (satisfies? identifiable-recur inter) (when (= (get-id inter) id) inter)
                                        :else (do (log-error :unsupported-operation (format "Cannot find next monitor, unsupported type, %s!" (type inter)) nil))))
                          ))))

(defn- swap-next-interaction-by-id!
  "Get the next interaction with next id already given"
  [id interactions]
  (fn [active-interaction]
    (let [nested-id-search (find-nested-next id interactions)]
      (println "nested-id" nested-id-search)
      nested-id-search)))

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
    (log-message (format "removing receiver %s, new receivers collection: %s" receiver newRecv))
    (cond
      (satisfies? interactable target-interaction)
      (swap! active-interaction (fn [inter] (->interaction (get-id target-interaction) (get-action target-interaction) (get-sender target-interaction) newRecv (get-next target-interaction)))))))


(defn- remove-receiver
  "Remove a receiver from the active monitor"
  ([active-interaction receiver]
   (remove-receiver active-interaction @active-interaction receiver))
  ([active-interaction current-interaction receiver]
   (let [recv (:receivers current-interaction)
         newRecv (vec (remove #{receiver} recv))]
     (log-message (format "removing receiver %s, new receivers collection: %s" receiver newRecv))
     (if (satisfies? interactable current-interaction)
       (swap! active-interaction (fn [inter] (->interaction (:id current-interaction) (:action current-interaction) (:sender current-interaction) newRecv (:next current-interaction))))
       (log-error :unsupported-operation (format "Cannot remove-receiver from interaction of type: %s, it should be atomic!" (type current-interaction)))))))

(defn- swap-active-interaction-by-atomic
  "Swap active interaction by atomic"
  ([active-interaction receiver interactions]
   (swap-active-interaction-by-atomic active-interaction @active-interaction receiver interactions))
  ([active-interaction target-interaction receiver interactions]
   (if (nil? receiver)
     (swap! active-interaction (swap-next-interaction-by-id! (get-next target-interaction) interactions))
     (if (multiple-receivers? target-interaction)
       (remove-receiver active-interaction target-interaction receiver)
       (swap! active-interaction (swap-next-interaction-by-id! (get-next target-interaction) interactions))))))

(defn- get-atomic-interaction
  "Check the atomic interaction"
  [sender receiver label active-interaction]
  (when (and (= (:action active-interaction) label) (= (:receivers active-interaction) receiver) (= (:sender active-interaction) sender)) active-interaction))

(defn- get-recursion-interaction
  "Check the first element in a recursion interaction"
  [sender receiver label active-interaction]
  (let [rec (get-recursion active-interaction)
        first-interaction (first rec)]
    (cond
      (satisfies? interactable first-interaction) (get-atomic-interaction sender receiver label first-interaction)
      (satisfies? branch first-interaction) (get-branch-interaction sender receiver label first-interaction)
      (satisfies? recursable first-interaction) (get-recursion-interaction sender receiver label first-interaction)
      :else (log-error :unsupported-operation "No correct next recursion monitor found"))))

(defn- get-branch-interaction
  "Check the atomic interaction"
  [sender receiver label active-interaction]
   (flatten
    (for [b (:branches active-interaction)]
      (let [first-in-branch (nth b 0)]
        (cond
          (satisfies? interactable first-in-branch) (get-atomic-interaction sender receiver label first-in-branch)
          (satisfies? branch first-in-branch) (get-branch-interaction sender receiver label first-in-branch)
          (satisfies? recursable first-in-branch) (get-recursion-interaction sender receiver label first-in-branch)
          :else (log-error :unsupported-operation "Cannot check operation on child branch construct!"))))))


(defn- get-first-valid-target-branch-interaction
  "Find the first interactable in (nested) branch constructs."
  [sender receiver label active-interaction]
  (first (filter some? (get-branch-interaction sender receiver label active-interaction))))

(defn- swap-active-interaction-by-branch
  "Swap active interaction by branch"
  ([sender receivers label active-interaction interactions]
   (let [target-interaction (get-first-valid-target-branch-interaction sender receivers label @active-interaction)]
     (log-message (format "Target interaction sender %s receivers %s action %s next %s" (:sender target-interaction) (:receivers target-interaction) (:action target-interaction) (:next target-interaction)))
     (if (multiple-receivers? target-interaction)
       (remove-receiver-from-branch active-interaction target-interaction receivers)
       (swap! active-interaction (swap-next-interaction-by-id! (:next target-interaction) interactions)))))
  ([sender receivers label active-interaction target-interaction interactions]
   (let [target (get-first-valid-target-branch-interaction sender receivers label target-interaction)]
     (log-message (format "Target interaction sender %s receivers %s action %s next %s" (:sender target) (:receivers target) (:action target) (:next target)))
     (if (multiple-receivers? target)
       (remove-receiver-from-branch active-interaction target receivers)
       (swap! active-interaction (swap-next-interaction-by-id! (:next target) interactions))))))

(defn- swap-active-interaction-by-recursion
  "Swap active interaction bu recursion"
  ([sender receivers label active-interaction interactions]
   (let [target-interaction (first (get-recursion @active-interaction))]
     (swap-active-interaction-by-recursion sender receivers label active-interaction target-interaction interactions)))
  ([sender receivers label active-interaction target-interaction interactions]
   (cond (satisfies? interactable target-interaction)
         (if (nil? receivers)
           (swap! active-interaction (swap-next-interaction-by-id! (get-next target-interaction) interactions))
           (if (multiple-receivers? target-interaction)
             (remove-receiver active-interaction target-interaction receivers)
             (swap! active-interaction (swap-next-interaction-by-id! (get-next target-interaction) interactions))))
         (satisfies? branch target-interaction)
         (let [first-in-branch (get-first-valid-target-branch-interaction sender receivers label target-interaction)]
           (log-message (format "Target interaction sender %s receivers %s action %s next %s" (:sender first-in-branch) (:receivers first-in-branch) (:action first-in-branch) (:next first-in-branch)))
           (if (multiple-receivers? first-in-branch)
             (remove-receiver-from-branch active-interaction first-in-branch receivers)
             (swap! active-interaction (swap-next-interaction-by-id! (:next first-in-branch) interactions))))
         (satisfies? recursable target-interaction)
         (swap-active-interaction-by-recursion sender receivers label active-interaction (first (get-recursion target-interaction)) interactions)
         :else (log-error :unsupported-operation "Cannot swap the interaction, unknown type!"))))

(defn- apply-interaction-to-mon
  "Apply new interaction"
  ([sender receivers label active-interaction interactions]
   (apply-interaction-to-mon sender receivers label active-interaction @active-interaction interactions))
  ([sender receivers label active-interaction target-interaction interactions]
   (log-message (format "Applying: label %s, receiver %s." label receivers))
   (cond
     (and (satisfies? interactable target-interaction) (check-atomic-interaction label target-interaction))
     (swap-active-interaction-by-atomic active-interaction target-interaction receivers interactions)
     (and (satisfies? branch target-interaction) (check-branch-interaction label target-interaction))
     (swap-active-interaction-by-branch sender receivers label active-interaction target-interaction interactions)
     (and (satisfies? recursable target-interaction) (check-recursion-interaction label target-interaction))
     (swap-active-interaction-by-recursion sender receivers label active-interaction target-interaction interactions)
     (satisfies? identifiable-recur target-interaction)
     (apply-interaction-to-mon sender receivers label active-interaction (find-nested-next (get-next target-interaction) interactions) interactions)
     :else (log-error :unsupported-operation (format "Unsupported type of interaction to apply %s!" (type target-interaction)))
     )))

(defn- contains-value?
  "Does the vector contain a value?"
  [element coll]
  (boolean (some #(= element %) coll)))

(defn- is-valid-interaction?
  "Is the given interaction valid compared to the active-interaction of the monitor"
  [sender receivers label active-interaction]
  ;(log-message (format "input = %s %s %s" sender receivers label))
  ;(log-message (format "active  = %s %s %s" (:sender active-interaction) (:receivers active-interaction) (:action active-interaction)))
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
  [sender receivers label active-interaction interactions]
  (cond
    (satisfies? interactable active-interaction)
    (is-valid-interaction? sender receivers label active-interaction)
    (satisfies? branch active-interaction)
    (> (count (filter true? (flatten (for [b (:branches active-interaction)] (is-valid-communication? sender receivers label (nth b 0) interactions))))) 0)
    (satisfies? recursable active-interaction)
    (is-valid-communication? sender receivers label (first (get-recursion active-interaction)) interactions)
    (satisfies? identifiable-recur active-interaction)
    (is-valid-communication? sender receivers label (find-nested-next (get-next active-interaction) interactions) interactions)
    :else
    (do (log-error :unsupported-operation (format "Unsupported communication type: Communication invalid, type: %s" (type active-interaction)))
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
  (valid-interaction? [this sender receivers label] (is-valid-communication? sender receivers label @active-interaction interactions)))

