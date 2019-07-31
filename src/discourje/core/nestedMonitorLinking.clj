;nestedMonitorLinking.clj
(in-ns 'discourje.core.async)

(declare nest-mep)
(defn- assoc-interaction [nth-i it]
  (cond
    (or (satisfies? interactable it) (satisfies? identifiable-recur it))
    (assoc nth-i :next it)
    (satisfies? branchable it)
    (let [branches (for [b (get-branches it)] (nest-mep (conj b (:next it))))]
      (assoc nth-i :next (assoc (assoc it :next nil) :branches branches)))
    (satisfies? recursable it)
    (let [rec (nest-mep (get-recursion it))]
      (assoc nth-i :next (assoc (assoc it :next nil) :recursion rec)))))

(defn- assoc-last-interaction [nth-i]
  (let [last nth-i
        next (:next nth-i)]
    (cond
      (satisfies? branchable last)
      (let [branches (for [b (get-branches last)] (nest-mep (conj b next)))]
        (assoc (assoc last :next nil) :branches branches))
      (satisfies? recursable last)
      (let [rec (nest-mep (get-recursion last))]
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
          (let [linked (assoc-interaction (nth interactions i) it)]
            (recur (- i 1) linked))))
      (cond
        (satisfies? interactable (first interactions))
        (first interactions)
        (satisfies? branchable (first interactions))
        (assoc-last-interaction (first interactions))
        (satisfies? recursable (first interactions))
        (println "FAILED RECURSION")
        ))))
