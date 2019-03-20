;interactionLinking.clj
(in-ns 'discourje.core.async)

(defn replace-last-in-vec
  "Replace the last value in a vector and return the new vector."
  [coll x]
  (conj (pop coll) x))

(defn assoc-next-nested-choice
  "Assoc nested choice next field recursively"
  [linked-i inter]
  (assoc linked-i :branches
                  (vec
                    (for [b (get-branches linked-i)]
                      (if (satisfies? branchable (last b))
                        (replace-last-in-vec b (assoc-next-nested-choice (last b) inter))
                        (if-not (satisfies? identifiable-recur (last b))
                          (replace-last-in-vec b (assoc (last b) :next (get-id inter)))
                          b))))))

(defn- find-nested-recur
  "Finds the next interaction based on id, nested in choices"
  [name option interactions]
  (first (filter some? (for [inter interactions]
                         (cond (satisfies? identifiable-recur inter) (when (and (= (get-name inter) name) (= (get-option option))) inter)
                               (satisfies? branchable inter) (let [branches (get-branches inter)
                                                               searches (for [b branches] (find-nested-recur name option b))]
                                                           (first searches))
                               (satisfies? recursable inter) (first (find-nested-recur name option (get-recursion inter))))))))

(defn- replace-nested-recur
  "Search and replace a recur-identifiers next ID with the given id"
  [name id option interactions]
  (let [prot (vec (for [inter interactions]
                    (cond (satisfies? identifiable-recur inter) (if (and (= (get-name inter) name) (= (get-option inter) option)) (assoc inter :next id) inter)
                          (satisfies? branchable inter) (let [branches (get-branches inter)
                                                          searches (for [b branches] (replace-nested-recur name id option b))]
                                                      (assoc inter :branches (vec searches)))
                          (satisfies? recursable inter) (assoc inter :recursion (replace-nested-recur name id option (get-recursion inter)))
                          :else inter)))]
    (vec prot)))


(defn assoc-next-nested-do-recur
  "Link do-recur"
  [linked-i]
  (let [name (get-name linked-i)
        prot (vec (replace-nested-recur name (get-id linked-i) :recur (get-recursion linked-i)))]
    (assoc linked-i :recursion prot)))

(defn assoc-next-nested-end-recur
  "Link end recur"
  [linked-i current-inter]
  (let [name (get-name linked-i)
        prot (vec (replace-nested-recur name (get-id current-inter) :end (get-recursion linked-i)))]
    (assoc linked-i :recursion prot)))

(defn- link-interactions
  ([protocol]
   (let [interactions (get-interactions protocol)
         helper-vec (atom [])
         linked-interactions (atom [])]
     (link-interactions interactions helper-vec linked-interactions)))
  ([interactions helper-vec linked-interactions]
   (do (doseq [inter interactions]
         (cond
           (satisfies? recursable inter)
           (let [recursion-help-vec (atom [])
                 linked-recursion-interactions (atom [])
                 recured-interactions (link-interactions (get-recursion inter) recursion-help-vec linked-recursion-interactions)
                 last-helper-mon (last @helper-vec)
                 linked-i (if (nil? last-helper-mon) nil (assoc last-helper-mon :next (get-id inter)))
                 new-recursion (->recursion (get-id inter) (get-name inter) recured-interactions nil)
                 recured-new-recursion (assoc-next-nested-do-recur new-recursion)]
             (swap! helper-vec conj recured-new-recursion)
             (when-not (nil? last-helper-mon)
               (cond
                 (satisfies? branchable linked-i) (swap! linked-interactions conj (assoc-next-nested-choice linked-i inter))
                 (satisfies? recursable linked-i) (let [ended-linked-i (assoc-next-nested-end-recur linked-i inter)]
                                                    (swap! linked-interactions conj ended-linked-i))
                 :else (swap! linked-interactions conj linked-i)))
             )
           (satisfies? branchable inter)
           (let [branched-interactions
                 (for [branch (get-branches inter)]
                   (let [branch-help-vec (atom [])
                         linked-branch-interactions (atom [])]
                     (link-interactions branch branch-help-vec linked-branch-interactions)))
                 last-helper-mon (last @helper-vec)
                 linked-i (if (nil? last-helper-mon) nil (assoc last-helper-mon :next (get-id inter)))
                 new-choice (->branch (get-id inter) branched-interactions nil)]
             (swap! helper-vec conj new-choice)
             (when-not (nil? last-helper-mon)
               (cond
                 (satisfies? branchable linked-i) (swap! linked-interactions conj (assoc-next-nested-choice linked-i inter))
                 (satisfies? recursable linked-i) (let [ended-linked-i (assoc-next-nested-end-recur linked-i inter)]
                                                    (swap! linked-interactions conj ended-linked-i))
                 :else (swap! linked-interactions conj linked-i)))
             )
           (empty? @helper-vec) (swap! helper-vec conj inter)
           (or (satisfies? interactable inter) (satisfies? identifiable-recur inter))
           (let [i (last @helper-vec)
                 linked-i (assoc i :next (get-id inter))]
             (swap! helper-vec conj inter)
             (cond
               (satisfies? branchable linked-i) (swap! linked-interactions conj (assoc-next-nested-choice linked-i inter))
               (satisfies? recursable linked-i) (let [ended-linked-i (assoc-next-nested-end-recur linked-i inter)]
                                                  (swap! linked-interactions conj ended-linked-i))
               :else (swap! linked-interactions conj linked-i)))
           ))
       (swap! linked-interactions conj (last @helper-vec))
       @linked-interactions)))