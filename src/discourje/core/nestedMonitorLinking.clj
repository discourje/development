;nestedMonitorLinking.clj
(in-ns 'discourje.core.async)
(declare nest-mep)

(defprotocol mappable-rec
  (get-rec-name [this])
  (get-initial-mapping [this])
  (get-mapped-rec [this mapping]))

(defn apply-mapping-to-rec [rec mapping]
  (if (nil? mapping)
    rec
    (apply-rec-mapping rec mapping)))

(defrecord rec-table-entry [name initial-mapping rec]
  mappable-rec
  (get-rec-name [this] name)
  (get-initial-mapping [this] initial-mapping)
  (get-mapped-rec [this mapping] (apply-mapping-to-rec rec mapping)))

(defn create-rec-table-entry [inter]
  (if (vector? (get-name inter))
    (->rec-table-entry (first (get-name inter)) (second (get-name inter)) (get-recursion inter))
    (->rec-table-entry (get-name inter) nil (get-recursion inter))))

(defn- assoc-to-rec-table [rec-table inter]
  (if (and (satisfies? recursable inter) (not (vector? (get-recursion inter))))
    (let [entry (create-rec-table-entry inter)]
      (when (or (nil? ((get-rec-name entry) @rec-table)) (empty? ((get-rec-name entry) @rec-table)))
        (swap! rec-table assoc (get-rec-name entry) (get-mapped-rec entry (get-initial-mapping entry))))
      (get-mapped-rec entry (get-initial-mapping entry)))
    inter))

(defn- assoc-interaction
  "assoc nth-i (index i-1) with it (index i) as next"
  [nth-i it rec-table]
  (cond
    (or (nil? it) (satisfies? identifiable-recur nth-i))
    nth-i
    (or (satisfies? interactable it) (satisfies? identifiable-recur it) (satisfies? closable it))
    (assoc nth-i :next it)
    (satisfies? branchable it)
    (let [branches (for [b (get-branches it)] (nest-mep (if-not (nil? (:next it)) (conj b (:next it)) b) rec-table))]
      (assoc nth-i :next (assoc (assoc it :next nil) :branches branches)))
    (satisfies? parallelizable it)
    (let [parallels (for [p (get-parallel it)] (nest-mep p rec-table))]
      (assoc nth-i :next (assoc it :parallels parallels)))
    (satisfies? recursable it)
    (let [rec (nest-mep (if-not (nil? (:next it)) (conj (get-recursion it) (:next it)) (get-recursion it)) rec-table)
          rec-result (assoc (assoc it :next nil) :recursion rec)]
      (assoc nth-i :next (assoc-to-rec-table rec-table rec-result)))
    :else (log-error :invalid-communication-type (format "Cannot link %s since this is an unknown communication type!" it)))
  )

(defn- assoc-last-interaction
  "assoc the last interaction in the list when it is of type branch parallel or recursion"
  [nth-i rec-table]
  (let [last nth-i
        next (:next nth-i)]
    (cond
      (satisfies? branchable last)
      (let [branches (for [b (get-branches last)] (nest-mep (if-not (nil? next) (conj b next) b) rec-table))]
        (assoc (assoc last :next nil) :branches branches))
      (satisfies? parallelizable last)
      (let [parallels (for [p (get-parallel last)] (nest-mep p rec-table))]
        (assoc last :parallels parallels))
      (satisfies? recursable last)
      (let [rec (nest-mep (if-not (nil? next) (conj (get-recursion last) next) (get-recursion last)) rec-table)
            result (assoc (assoc last :next nil) :recursion rec)]
        (assoc-to-rec-table rec-table result)))))

(defn nest-mep
  "assign all next keys in a given vector of interactions (note that choice, parallel and recursion make this function called recursively)"
  [interactions rec-table]
  (let [inter (when-not (nil? interactions)
                (if (>= (count interactions) 2)
                  (loop [i (- (count interactions) 2)
                         it (last interactions)]
                    (if (== 0 i)
                      (let [assoced (assoc-interaction (nth interactions i) it rec-table)
                            link (if (vector? assoced) (first assoced) assoced)]
                        (if (or (satisfies? branchable link) (satisfies? recursable link) (satisfies? parallelizable link))
                          (assoc-last-interaction link rec-table)
                          link))
                      (if (vector? interactions)
                        (let [linked (assoc-interaction (nth interactions i) it rec-table)]
                          (recur (- i 1) linked))
                        interactions)))
                  (cond
                    (or (satisfies? interactable (first interactions)) (satisfies? identifiable-recur (first interactions)) (satisfies? closable (first interactions)))
                    (first interactions)
                    (or (satisfies? branchable (first interactions)) (satisfies? recursable (first interactions)) (satisfies? parallelizable (first interactions)))
                    (assoc-last-interaction (first interactions) rec-table)
                    )))]
    (assoc-to-rec-table rec-table inter)))