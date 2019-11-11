;atomic construct
(in-ns 'discourje.core.async)

;;---------------------------------Sendable implementation-------------------------------------------------
(defn- is-valid-sendable-atomic?
  "Check if the interaction is valid for a send operation"
  [active-interaction sender receivers message]
  (when (and (is-valid-interaction? sender receivers message active-interaction)
             (false? (contains? (get-accepted-sends active-interaction) sender)))
    active-interaction))

(defn- assoc-sender
  "Assoc a sender to the accepted-sends set and return in"
  ([atomic sender is-found]
   (println "associng sender" sender "to" atomic)
   (reset! is-found true)
   (assoc-sender atomic sender))
  ([atomic sender]
   (assoc atomic :accepted-sends (conj (:accepted-sends atomic) sender))))

(defn- apply-sendable-atomic!
  "Send active interaction by atomic"
  [target-interaction pre-swap-interaction active-interaction sender]
  (if (nil? sender)
    (log-error :invalid-send (format "sender appears to be nil: %s %s" active-interaction target-interaction))
    (= (get-id (swap! active-interaction (fn [inter]
                                           (if (= (get-id inter) (get-id pre-swap-interaction))
                                             (assoc-sender target-interaction sender)
                                             inter)
                                           )))
       (get-id target-interaction))))

(defn- get-sendable-atomic
  "get the atomic interaction when it is valid for send"
  [active-interaction sender receiver message]
  (when (is-valid-sendable-atomic? active-interaction sender receiver message) active-interaction))

;;---------------------------------Receivable implementation-----------------------------------------------
(defn- multiple-receivers?
    "Does the monitor have multiple receivers?"
    [active-interaction]
    (and (instance? Seqable (:receivers active-interaction)) (> (count (:receivers active-interaction)) 1)))

(defn- is-multicast-atomic?
  "Does the monitor have multiple receivers?"
  [active-interaction message]
  (and (is-predicate-valid? message active-interaction) (multiple-receivers? active-interaction)))

(defn- remove-receivable-atomic!
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

(defn- apply-receivable-atomic!
  "Swap active interaction by atomic
  The end-protocol comparison indicates the protocol is terminated."
  [target-interaction pre-swap-interaction active-interaction receiver]
  (if (multiple-receivers? target-interaction)
    (remove-receivable-atomic! active-interaction target-interaction receiver)
    (let [swapped (swap! active-interaction (fn [inter]
                                              (if (= (get-id inter) (get-id pre-swap-interaction))
                                                (get-next target-interaction)
                                                inter)))]
      (= (if (nil? swapped) "end-protocol" (get-id swapped))
         (if (or (nil? target-interaction) (nil? (get-next target-interaction)))
           "end-protocol"
           (get-id (get-next target-interaction)))))))


(defn- get-receivable-atomic
  "Check the atomic interaction"
  [active-interaction sender receiver message]
  (println "inter="(interaction-to-string active-interaction) " sender =" sender " receivers =" receiver " message" message)
  (println "good receive?="(contains? (get-accepted-sends active-interaction) sender))
  (println "valid?" (is-valid-interaction? sender receiver message active-interaction))
  (when (and (contains? (get-accepted-sends active-interaction) sender) (is-valid-interaction? sender receiver message active-interaction))
    active-interaction))

;;---------------------------------Closable implementation-------------------------------------------------

(defn- is-valid-closable-atomic? [this]
  nil)

(defn- apply-closable-atomic! [this]
  nil)

(defn- get-closable-atomic [this]
  nil)