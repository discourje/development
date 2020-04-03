;;parallel construct
;(in-ns 'discourje.core.async.impl.monitor)
;
;(import clojure.lang.LazySeq)
;
;;;---------------------------------Sendable implementation-------------------------------------------------
;(defn- is-valid-sendable-parallel? [active-interaction monitor sender receivers message]
;  (when-let [_ (first (filter
;                        #(is-valid-sendable? % monitor sender receivers message) (get-parallel active-interaction)))]
;    active-interaction))
;
;(defn- get-sendable-parallel
;  "Check the atomic interaction"
;  [active-interaction monitor sender receivers message]
;  (when-let [_ (first (filter
;                        #(get-sendable % monitor sender receivers message) (get-parallel active-interaction)))]
;    active-interaction))
;
;(defn- set-send-on-parallel
;  "Assoc a sender to a nested parallel interaction"
;  [sender receivers message target-interaction monitor]
;  (let [pars (let [pars (get-parallel target-interaction)
;                   is-found (atom false)]
;               (for [p pars]
;                 (cond
;                   @is-found p
;                   (satisfies? parallelizable p)
;                   (set-send-on-parallel sender receivers message p monitor)
;                   (is-valid-sendable? p monitor sender receivers message)
;                   (let [valid (get-sendable p monitor sender receivers message)]
;                     (if (satisfies? parallelizable valid)
;                       (set-send-on-parallel sender receivers message valid monitor)
;                       (do (when (satisfies? identifiable-recur p)
;                             (get-rec monitor (get-name p) true))
;                           (assoc-sender valid sender is-found))))
;                   :else
;                   p
;                   )))]
;    (let [duplicate-par (first (filter some? (filter (fn [p] (= (get-id p) (get-id target-interaction))) pars)))]
;      (if (nil? duplicate-par)
;        (assoc target-interaction :parallels pars)
;        duplicate-par))))
;
;(defn- apply-sendable-parallel! [target-interaction pre-swap-interaction active-interaction monitor sender receivers message]
;  (swap! active-interaction
;         (fn [inter]
;           (if (= (get-id inter) (get-id target-interaction))
;             (set-send-on-parallel sender receivers message inter monitor)
;             (set-send-on-parallel sender receivers message target-interaction monitor))))
;  true)
;
;;;--------------------------------Receivable implementation------------------------------------------------
;(defn is-multicast-parallel? [active-interaction monitor message]
;  (first (filter #(is-multicast? % monitor message) (get-parallel active-interaction))))
;
;(defn- is-valid-receivable-parallel? [active-interaction monitor sender receivers message]
;  (when-let [_ (first (filter
;                        #(is-valid-receivable? % monitor sender receivers message) (get-parallel active-interaction)))]
;    active-interaction))
;
;(defn- get-receivable-parallel
;  "Check the atomic interaction"
;  [active-interaction monitor sender receivers message]
;  (when-let [_ (first (filter
;                        #(is-valid-receivable? % monitor sender receivers message) (get-parallel active-interaction)))]
;    active-interaction))
;
;(defn remove-from-parallel
;  "Remove an interaction from a parallel in a recursive fashion."
;  [sender receivers message target-interaction monitor]
;  (let [pars (flatten (filter some?
;                              (for [par (get-parallel target-interaction)]
;                                (let [is-found (atom false)
;                                      inter (cond
;                                              @is-found par
;                                              (satisfies? parallelizable par)
;                                              (remove-from-parallel sender receivers message par monitor)
;                                              (satisfies? interactable par)
;                                              (get-receivable-atomic par sender receivers message)
;                                              (satisfies? branchable par)
;                                              (get-receivable-branch par monitor sender receivers message)
;                                              (satisfies? identifiable-recur par)
;                                              (let [recursion (get-rec monitor (get-name par) false)
;                                                    valid-rec (get-receivable-recur-identifier recursion monitor sender receivers message)]
;                                                (if (nil? valid-rec)
;                                                  par
;                                                  (get-rec monitor (get-name par) true)))
;                                              :else
;                                              par)]
;                                  (if @is-found
;                                    inter
;                                    (if (nil? inter)
;                                      (if (satisfies? parallelizable par)
;                                        nil
;                                        par)
;                                      (cond
;                                        (satisfies? parallelizable inter)
;                                        (remove-from-parallel sender receivers message inter monitor)
;                                        (satisfies? interactable inter)
;                                        (do
;                                          (reset! is-found true)
;                                          (if (multiple-receivers? inter)
;                                            (assoc inter :receivers (vec (remove #{receivers} (:receivers inter))))
;                                            (get-next inter))
;                                          )
;                                        (or (satisfies? branchable inter) (satisfies? closable inter))
;                                        inter
;                                        (and (instance? LazySeq inter) (not (satisfies? interactable inter)))
;                                        (first (filter some? inter)))))))))]
;    (if (empty? pars)
;      (get-next target-interaction)
;      (assoc target-interaction :parallels pars))))
;
;(defn- apply-receivable-parallel! [target-interaction pre-swap-interaction active-interaction monitor sender receivers message]
;  (let [target-parallel-interaction (get-receivable-parallel target-interaction monitor sender receivers message)]
;    (swap! active-interaction
;           (fn [inter]
;             (let [target (if (= (get-id inter) (get-id target-parallel-interaction))
;                            (get-receivable-parallel inter monitor sender receivers message)
;                            target-parallel-interaction)]
;               (if (nil? target)
;                 inter
;                 (remove-from-parallel sender receivers message target monitor))))))
;  true)
;;;---------------------------------Closable implementation-------------------------------------------------
;
;(defn- is-valid-closable-parallel? [active-interaction monitor sender receiver]
;  (first (filter
;           #(is-valid-closable? % monitor sender receiver) (get-parallel active-interaction))))
;(defn- get-closable-parallel
;  "Check the atomic interaction"
;  [active-interaction monitor sender receiver]
;  (when-let [_ (first (filter
;                        #(is-valid-closable? % monitor sender receiver) (get-parallel active-interaction)))]
;    active-interaction))
;
;(defn remove-close-from-parallel
;  "Remove an interaction from a parallel in a recursive fashion."
;  [sender receivers target-interaction monitor]
;  (let [pars (flatten (filter some?
;                              (for [par (get-parallel target-interaction)]
;                                (let [is-found (atom false)
;                                      inter (cond
;                                              @is-found par
;                                              (satisfies? parallelizable par)
;                                              (remove-close-from-parallel sender receivers par monitor)
;                                              (is-valid-closable? par monitor sender receivers)
;                                              (get-closable par monitor sender receivers)
;                                              :else
;                                              par)]
;                                  (if @is-found
;                                    inter
;                                    (if (nil? inter)
;                                      (if (satisfies? parallelizable par)
;                                        nil
;                                        par)
;                                      (cond
;                                        (satisfies? parallelizable inter)
;                                        (remove-close-from-parallel sender receivers inter monitor)
;                                        (satisfies? closable inter)
;                                        (get-next inter)
;                                        (or (satisfies? branchable inter) (satisfies? interactable inter))
;                                        inter
;                                        (and (instance? LazySeq inter) (not (satisfies? interactable inter)))
;                                        (first (filter some? inter)))))))))]
;    (if (empty? pars)
;      (get-next target-interaction)
;      (assoc target-interaction :parallels pars))))
;
;(defn- apply-closable-parallel! [target-interaction pre-swap-interaction active-interaction monitor channel]
;  (let [target-parallel-interaction (get-closable-parallel target-interaction monitor (get-provider channel) (get-consumer channel))]
;    (swap! active-interaction
;           (fn [inter]
;             (let [target (if (= (get-id inter) (get-id target-parallel-interaction))
;                            (get-closable-parallel inter monitor (get-provider channel) (get-consumer channel))
;                            target-parallel-interaction)]
;               (if (nil? target)
;                 inter
;                 (remove-close-from-parallel (get-provider channel) (get-consumer channel) target monitor))))))
;  true)