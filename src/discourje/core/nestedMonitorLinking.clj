;nestedMonitorLinking.clj
(in-ns 'discourje.core.async)

(declare nest-mep)
(defn- assoc-interaction
  "assoc nth-i (index i-1) with it (index i) as next"
  [nth-i it]
  (cond
    (or (nil? it) (satisfies? identifiable-recur nth-i))
    nth-i
    (or (satisfies? interactable it) (satisfies? identifiable-recur it))
    (assoc nth-i :next it)
    (satisfies? branchable it)
    (let [branches (for [b (get-branches it)] (nest-mep (if-not (nil? (:next it)) (conj b (:next it)) b)))]
      (assoc nth-i :next (assoc (assoc it :next nil) :branches branches)))
    (satisfies? parallelizable it)
    (let [parallels (for [p (get-parallel it)] (nest-mep (if-not (nil? (:next it)) (conj p (:next it)) p)))]
      (assoc nth-i :next (assoc (assoc it :next nil) :parallels parallels)))
    (satisfies? recursable it)
    (let [rec (nest-mep (if-not (nil? (:next it)) (conj (get-recursion it) (:next it)) (get-recursion it)))]
      (assoc nth-i :next (assoc (assoc it :next nil) :recursion rec)))
    :else (log-error :invalid-communication-type (format ("Cannot link %s since this is an unknown communication type!" it)))))

(defn- assoc-last-interaction
  "assoc the last interaction in the list when it is of type branch parallel or recursion"
  [nth-i]
  (let [last nth-i
        next (:next nth-i)]
    (cond
      (satisfies? branchable last)
      (let [branches (for [b (get-branches last)] (nest-mep (if-not (nil? next) (conj b next) b)))]
        (assoc (assoc last :next nil) :branches branches))
      (satisfies? parallelizable last)
      (let [parallels (for [p (get-parallel last)] (nest-mep (if-not (nil? next) (conj p next) p)))]
        (assoc (assoc last :next nil) :parallels parallels))
      (satisfies? recursable last)
      (let [rec (nest-mep (if-not (nil? next) (conj (get-recursion last) next) (get-recursion last)))]
        (assoc (assoc last :next nil) :recursion rec)))))

(defn nest-mep
  "assign all next keys in a given vector of interactions (note that choice, parallel and recursion make this function called recursively)"
  [interactions]
  (when-not (nil? interactions)
    (if (>= (count interactions) 2)
      (loop [i (- (count interactions) 2)
             it (last interactions)]
        (if (== 0 i)
          (let [link (assoc-interaction (nth interactions i) it)]
            (if (or (satisfies? branchable link) (satisfies? recursable link) (satisfies? parallelizable link))
              (assoc-last-interaction link)
              link))
          (if (vector? interactions)
            (let [linked (assoc-interaction (nth interactions i) it)]
              (recur (- i 1) linked))
            interactions)))
      (cond
        (or (satisfies? interactable (first interactions)) (satisfies? identifiable-recur (first interactions)))
        (first interactions)
        (or (satisfies? branchable (first interactions)) (satisfies? recursable (first interactions)) (satisfies? parallelizable (first interactions)))
        (assoc-last-interaction (first interactions))))))