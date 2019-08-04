;nestedMonitorLinking.clj
(in-ns 'discourje.core.async)

(declare nest-mep)
(defn- assoc-interaction [nth-i it]
  (cond
    (or (nil? it) (satisfies? identifiable-recur nth-i))
    nth-i
    (or (satisfies? interactable it) (satisfies? identifiable-recur it))
    (assoc nth-i :next it)
    (satisfies? branchable it)
    (let [branches (for [b (get-branches it)] (nest-mep (if-not (nil? (:next it)) (conj b (:next it)) b)))]
      (assoc nth-i :next (assoc (assoc it :next nil) :branches branches)))
    (satisfies? recursable it)
    (let [rec (nest-mep (if-not (nil? (:next it)) (conj (get-recursion it) (:next it)) (get-recursion it)))]
      (assoc nth-i :next (assoc (assoc it :next nil) :recursion rec)))))

(defn- assoc-last-interaction [nth-i]
  (let [last nth-i
        next (:next nth-i)]
    (cond
      (satisfies? branchable last)
      (let [branches (for [b (get-branches last)] (nest-mep (if-not (nil? next) (conj b next) b)))]
        (assoc (assoc last :next nil) :branches branches))
      (satisfies? recursable last)
      (let [rec (nest-mep (if-not (nil? next) (conj (get-recursion last) next) (get-recursion last)))]
        (assoc (assoc last :next nil) :recursion rec)))))

(defn nest-mep [interactions]
  (when-not (nil? interactions)
    (if (>= (count interactions) 2)
      (loop [i (- (count interactions) 2)
             it (last interactions)]
        (if (== 0 i)
          (let [link (assoc-interaction (nth interactions i) it)]
            (if (or (satisfies? branchable link) (satisfies? recursable link))
              (assoc-last-interaction link)
              link))
            (if (vector? interactions)
              (let [linked (assoc-interaction (nth interactions i) it)]
                (recur (- i 1) linked))
              interactions)))
        (cond
          (or (satisfies? interactable (first interactions)) (satisfies? identifiable-recur (first interactions)))
          (first interactions)
          (or (satisfies? branchable (first interactions)) (satisfies? recursable (first interactions)))
          (assoc-last-interaction (first interactions))
          ))))
