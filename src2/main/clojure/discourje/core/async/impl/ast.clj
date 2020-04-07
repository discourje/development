(ns discourje.core.async.impl.ast)

(defn println-true [& more]
  (println more)
  true)

;;;;
;;;; Discourje
;;;;

(defprotocol Discourje)

;;;;
;;;; Discourje: Roles
;;;;

(def role-names (atom {}))

(defn put-role-name! [k name]
  {:pre [(keyword? k)
         (string? name)]}
  (swap! role-names (fn [m] (into m {k name}))))

(defn get-role-name [k]
  (get @role-names k))

(defrecord Role [name-expr index-exprs])

(defn role? [x]
  (instance? Role x))

(defn role
  ([expr]
   {:pre [(or (not (coll? expr)) (seq? expr))]}
   (cond
     (not (coll? expr)) (role expr [])
     (seq? expr) (role (first expr) (vec (rest expr)))))

  ([name-expr index-exprs]
   {:pre [(or (string? name-expr) (symbol? name-expr) (keyword? name-expr))
          (vector? index-exprs)]}
   (->Role name-expr index-exprs)))

;;;;
;;;; Discourje: Actions
;;;;

(defrecord Predicate [expr])

(defn predicate? [x]
  (instance? Predicate x))

(defn predicate [expr]
  {:pre []}
  (->Predicate expr))

(defrecord Action [type predicate sender receiver]
  Discourje)

(def action-types #{:send :receive :close})

(defn send [predicate sender receiver]
  {:pre [(predicate? predicate)
         (role? sender)
         (role? receiver)]}
  (->Action :send predicate sender receiver))

(defn receive [predicate sender receiver]
  {:pre [(predicate? predicate)
         (role? sender)
         (role? receiver)]}
  (->Action :receive predicate sender receiver))

(defn close [sender receiver]
  {:pre [(role? sender)
         (role? receiver)]}
  (->Action :close (predicate '(fn [_] true)) sender receiver))

;;;;
;;;; Discourje: Nullary operators
;;;;

(defrecord Nullary [type]
  Discourje)

(defn end []
  (->Nullary :end))

;;;;
;;;; Discourje: Multiary operators
;;;;

(defrecord Multiary [type branches]
  Discourje)

(defn choice [branches]
  (->Multiary :choice branches))
(defn parallel [branches]
  (->Multiary :parallel branches))

;;;;
;;;; Discourje: Conditional operators
;;;;

(defrecord If [type condition branch1 branch2]
  Discourje)

(defn if-then-else [condition branch1 branch2]
  (->If :if condition branch1 branch2))
(defn if-then [condition branch]
  (if-then-else condition branch (end)))

;;;;
;;;; Discourje: Recursion operators
;;;;

(defrecord Loop [type name vars exprs body]
  Discourje)

(defn loop
  ([name bindings body]
   (let [vars (take-nth 2 bindings)
         exprs (take-nth 2 (rest bindings))]
     (loop name vars exprs body)))
  ([name vars exprs body]
   (->Loop :loop name vars exprs body)))

(defrecord Recur [type name exprs]
  Discourje)

(defn recur [name exprs]
  (->Recur :recur name exprs))

;;;;
;;;; Discourje: Registry operators
;;;;

(def registry (atom {}))

(defn register! [name vars body]
  (swap! registry
         (fn [m]
           (if (contains? m name)
             (update m name #(into % {(count vars) {:vars vars, :body body}}))
             (into m {name {(count vars) {:vars vars, :body body}}})))))

;;;;
;;;; TODO: Old code, some of which must be salvaged (but probably in a different namespace)
;;;;

;(defn unique-cartesian-product
;  "Generate channels between all participants and filters out duplicates e.g.: A<->A"
;  [x y]
;  (filter some?
;          (for [x x y y]
;            (when (not (identical? x y))
;              (vector x y)))))
;
;(defn- find-all-role-pairs
;  "List all sender and receivers in the protocol"
;  [protocol result]
;  (let [result2 (conj result [])]
;    (conj result2
;          (flatten
;            (for [element protocol]
;              (cond
;                (satisfies? discourje.core.async.impl.dsl.syntax/recursable element)
;                (if (vector? (get-name element))
;                  (let [mapping (second (get-name element))
;                        mapping-vals (if (map? mapping)
;                                       (vals mapping)
;                                       (vals (apply hash-map mapping)))
;                        cartesian-product (unique-cartesian-product mapping-vals mapping-vals)
;                        mapped-channels (vec (for [pair cartesian-product] {:sender (first pair) :receivers (second pair)}))
;                        result3 (conj result2 (flatten mapped-channels))]
;                    (conj result3 (flatten (find-all-role-pairs (get-recursion element) result3)))
;                    )
;                  (conj result2 (flatten (find-all-role-pairs (get-recursion element) result2))))
;                (satisfies? discourje.core.async.impl.dsl.syntax/branchable element)
;                (let [branched-interactions (for [branch (get-branches element)] (find-all-role-pairs branch result2))]
;                  (conj result2 (flatten branched-interactions)))
;                (satisfies? discourje.core.async.impl.dsl.syntax/parallelizable element)
;                (let [parallel-interactions (for [p (get-parallel element)] (find-all-role-pairs p result2))]
;                  (conj result2 (flatten parallel-interactions)))
;                (satisfies? discourje.core.async.impl.dsl.syntax/interactable element)
;                (if (or (keyword? (get-sender element)) (or (and (vector? (get-receivers element)) (first (filter true? (filter keyword? (get-receivers element)))))) (keyword? (get-receivers element)))
;                  result2
;                  (if (vector? (get-receivers element))
;                    (conj result2 (flatten (vec (for [rsvr (get-receivers element)] {:sender (get-sender element) :receivers rsvr}))))
;                    (conj result2 {:sender (get-sender element) :receivers (get-receivers element)})))
;                (satisfies? discourje.core.async.impl.dsl.syntax/closable element)
;                result2
;                (satisfies? discourje.core.async.impl.dsl.syntax/identifiable-recur element)
;                result2
;                :else
;                (log-error :invalid-communication-type "Cannot find roles pairs for type:" element)))))))
;
;(defn get-distinct-role-pairs
;  "Get minimum amount of distinct sender and receivers pairs needed to implement the given protocol"
;  [interactions]
;  (vec (distinct (filter some? (flatten (find-all-role-pairs interactions []))))))
;
;(defn- get-rec-from-table [name rec-table save-mapping]
;  (if (vector? name)
;    (let [entry ((first name) @rec-table)
;          mapping (second name)
;          new-mapping (create-new-mapping (get-current-mapping entry) mapping)
;          new-recursion (get-mapped-rec entry mapping)]
;      (when (true? save-mapping)
;        (swap! rec-table assoc (first name) (assoc entry :initial-mapping new-mapping)))
;      new-recursion)
;    (get-mapped-rec (name @rec-table) nil)))
;
;(defprotocol RecursiveInteractions
;  (get-active-interaction [this])
;  (get-rec [this name save-mapping]))
;
;(defrecord Spec [id active-interaction recursion-set]
;  RecursiveInteractions
;  (get-active-interaction [this] @active-interaction)
;  (get-rec [this name save-mapping] (get-rec-from-table name recursion-set save-mapping)))
;
;(defn generate-spec
;  "Generate the spec based on the given protocol"
;  [protocol]
;  (let [rec-table (atom {})
;        linked-interactions (nest-mep (get-interactions protocol) rec-table)]
;    (->Spec (uuid) (atom linked-interactions) rec-table)))
;
;(import clojure.lang.Seqable)
;
;(defn- map-value! [original mapping]
;  (let [map-fn (fn [org mapp] (if (keyword? org)
;                                (org mapp)
;                                org))]
;    (if (vector? original)
;      (vec (for [rsvr original] (map-fn rsvr mapping)))
;      (map-fn original mapping))))
;
;(defn is-predicate-valid?
;  "Is the predicate in the monitor valid compared to the message or label (when given)"
;  [message active-interaction]
;  ((get-predicate active-interaction) message))
;
;(defn- contains-value?
;  "Does the vector contain a value?"
;  [element coll]
;  (when (instance? Seqable coll)
;    (boolean (some #(= element %) coll))))
;
;(defn- is-valid-interaction?
;  "Is the given interaction valid compared to the active-interaction of the monitor"
;  [sender receivers message active-interaction]
;  (and
;    (= sender (:sender active-interaction))
;    (is-predicate-valid? message active-interaction)
;    (and (if (instance? Seqable (:receivers active-interaction))
;           (or (contains-value? receivers (:receivers active-interaction)) (= receivers (:receivers active-interaction)))
;           (or (= receivers (:receivers active-interaction)) (contains-value? (:receivers active-interaction) receivers))))))
;
;(defn- interaction-to-string
;  "Stringify an interaction, returns empty string if the given interaction is nil"
;  [interaction]
;  (if (satisfies? stringify interaction) (to-string interaction) interaction))
;
;(declare nest-mep)
;
;(defprotocol mappable-rec
;  (get-rec-name [this])
;  (get-current-mapping [this])
;  (get-mapped-rec [this mapping]))
;
;(defn create-new-mapping [initial-mapping mapping]
;  (let [map-vec (vec initial-mapping)
;        continue-mapping (if (map? mapping)
;                           (vec (flatten (vec mapping)))
;                           mapping)
;        new-vec (atom [])]
;    (loop [index 0]
;          (reset! new-vec (conj @new-vec (assoc (nth map-vec index) 1 ((nth continue-mapping index) initial-mapping))))
;          (when (< index (- (count map-vec) 1))
;            (recur (+ index 1))))
;    (apply array-map (flatten @new-vec))))
;
;(defrecord rec-table-entry [name initial-mapping rec]
;  mappable-rec
;  (get-rec-name [this] name)
;  (get-current-mapping [this] initial-mapping)
;  (get-mapped-rec [this mapping] (cond
;                                   (nil? initial-mapping)
;                                   rec
;                                   (or (nil? mapping) (not= (count (keys initial-mapping)) (count (keys mapping))))
;                                   (apply-rec-mapping rec initial-mapping)
;                                   :else
;                                   (apply-rec-mapping rec (create-new-mapping initial-mapping mapping))))
;  )
;
;(defn- create-rec-table-entry [inter]
;  (if (vector? (get-name inter))
;    (->rec-table-entry (first (get-name inter))
;                       (if (map? (second (get-name inter)))
;                         (second (get-name inter))
;                         (apply array-map (second (get-name inter))))
;                       (get-recursion inter))
;    (->rec-table-entry (get-name inter) nil (get-recursion inter))))
;
;(defn- get-initial-mapping-keys [initial-mapping]
;  (vec (keys (if (map initial-mapping)
;               initial-mapping
;               (apply array-map initial-mapping)))))
;
;(defn- assoc-to-rec-table [rec-table inter]
;  (if (and (satisfies? recursable inter) (not (vector? (get-recursion inter))))
;    (let [entry (create-rec-table-entry inter)]
;      (when (or (nil? ((get-rec-name entry) @rec-table)) (empty? ((get-rec-name entry) @rec-table)))
;        (swap! rec-table assoc (get-rec-name entry) entry))
;      (get-mapped-rec entry (get-initial-mapping-keys (get-current-mapping entry))))
;    inter))
;
;(defn- assoc-interaction
;  "assoc nth-i (index i-1) with it (index i) as next"
;  [nth-i it rec-table]
;  (cond
;    (or (nil? it) (satisfies? identifiable-recur nth-i))
;    nth-i
;    (or (satisfies? interactable it) (satisfies? identifiable-recur it) (satisfies? closable it))
;    (assoc nth-i :next it)
;    (satisfies? branchable it)
;    (let [branches (for [b (get-branches it)] (nest-mep (if-not (nil? (:next it)) (conj b (:next it)) b) rec-table))]
;      (assoc nth-i :next (assoc (assoc it :next nil) :branches branches)))
;    (satisfies? parallelizable it)
;    (let [parallels (for [p (get-parallel it)] (nest-mep p rec-table))]
;      (assoc nth-i :next (assoc it :parallels parallels)))
;    (satisfies? recursable it)
;    (let [rec (nest-mep (if-not (nil? (:next it)) (conj (get-recursion it) (:next it)) (get-recursion it)) rec-table)
;          rec-result (assoc (assoc it :next nil) :recursion rec)]
;      (assoc nth-i :next (assoc-to-rec-table rec-table rec-result)))
;    :else (log-error :invalid-communication-type (format "Cannot link %s since this is an unknown communication type!" it)))
;  )
;
;(defn- assoc-last-interaction
;  "assoc the last interaction in the list when it is of type branch parallel or recursion"
;  [nth-i rec-table]
;  (let [last nth-i
;        next (:next nth-i)]
;    (cond
;      (satisfies? branchable last)
;      (let [branches (for [b (get-branches last)] (nest-mep (if-not (nil? next) (conj b next) b) rec-table))]
;        (assoc (assoc last :next nil) :branches branches))
;      (satisfies? parallelizable last)
;      (let [parallels (for [p (get-parallel last)] (nest-mep p rec-table))]
;        (assoc last :parallels parallels))
;      (satisfies? recursable last)
;      (let [rec (nest-mep (if-not (nil? next) (conj (get-recursion last) next) (get-recursion last)) rec-table)
;            result (assoc (assoc last :next nil) :recursion rec)]
;        (assoc-to-rec-table rec-table result)))))
;
;(defn nest-mep
;  "assign all next keys in a given vector of interactions (note that choice, parallel and recursion make this function called recursively)"
;  [interactions rec-table]
;  (let [inter (when-not (nil? interactions)
;                (if (>= (count interactions) 2)
;                  (loop [i (- (count interactions) 2)
;                         it (last interactions)]
;                        (if (== 0 i)
;                          (let [assoced (assoc-interaction (nth interactions i) it rec-table)
;                                link (if (vector? assoced) (first assoced) assoced)]
;                            (if (or (satisfies? branchable link) (satisfies? recursable link) (satisfies? parallelizable link))
;                              (assoc-last-interaction link rec-table)
;                              link))
;                          (if (vector? interactions)
;                            (let [linked (assoc-interaction (nth interactions i) it rec-table)]
;                              (recur (- i 1) linked))
;                            interactions)))
;                  (cond
;                    (or (satisfies? interactable (first interactions)) (satisfies? identifiable-recur (first interactions)) (satisfies? closable (first interactions)))
;                    (first interactions)
;                    (or (satisfies? branchable (first interactions)) (satisfies? recursable (first interactions)) (satisfies? parallelizable (first interactions)))
;                    (assoc-last-interaction (first interactions) rec-table)
;                    )))]
;    (assoc-to-rec-table rec-table inter)))

;;;;
;;;; Aldebaran
;;;;

(defprotocol Aldebaran)

(defrecord Graph [v0 edges]
  Aldebaran)

(defn graph [v0 edges]
  (->Graph v0
           (clojure.core/loop [todo edges
                               result {}]
             (if (empty? todo)
               result
               (recur (rest todo)
                      (let [transition (first todo)
                            source (nth transition 0)
                            label (nth transition 1)
                            target (nth transition 2)]

                        (if (and (contains? result source)
                                 (contains? (get result source) label))
                          (update result source #(merge-with into % {label [target]}))
                          (merge-with into result {source {label [target]}}))))))))