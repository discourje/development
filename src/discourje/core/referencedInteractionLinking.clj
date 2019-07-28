;referencedInteractionLinking.clj
(in-ns 'discourje.core.async)

(defn- assoc-next-atom "assoc the inter to the :next key of the previous" [prev inter]
  (assoc prev :next inter))

(defn- replace-last-in-vec
  "Replace the last value in a vector and return the new vector."
  [coll x]
  (conj (pop coll) x))

(defn link-interactions-by-reference
  ([protocol]
   (link-interactions-by-reference 1 (get-interactions protocol))
    )
  ([index interactions]
   (let [first-interaction (atom (first interactions))]
     (when (< 1 (count interactions))
       (loop [i index
              new-inter first-interaction]
         (let [inter (atom (nth interactions i))]
           (cond
             (satisfies? branchable @new-inter)
             (doseq [b (get-branches @new-inter)]
               (loop [current-branch-inter b]
                 (if (nil? (get-next @current-branch-inter))
                   (swap! current-branch-inter assoc-next-atom inter)
                   (recur (get-next @current-branch-inter)))))
             (satisfies? recursable @new-inter)
             (loop [next-interaction (get-recursion @new-inter)]
               (if (and (satisfies? identifiable-recur @next-interaction) (= (get-name @next-interaction) (get-name @new-inter)))
                 (swap! next-interaction assoc-next-atom new-inter)
                 (recur (get-next @next-interaction))))
             (or (satisfies? interactable (nth interactions i)) (satisfies? identifiable-recur (nth interactions i)))
             (swap! new-inter assoc-next-atom inter)
             (satisfies? branchable (nth interactions i))
             (let [linked-branches (for [b (get-branches (nth interactions i))] (link-interactions-by-reference 1 b))]
               (swap! new-inter assoc-next-atom inter)
               (swap! inter assoc :branches linked-branches)
               )
             (satisfies? recursable (nth interactions i))
             (let [linked-recursion (link-interactions-by-reference 1 (get-recursion (nth interactions i)))]
               (swap! new-inter assoc-next-atom inter)
               (swap! inter assoc :recursion linked-recursion))
             :else
             (log-error :unsupported-reference-link "unsupported reference link interaction"))
           (when (true? (< i (- (count interactions) 1)))
             (recur (+ 1 i) inter)))))
     first-interaction)))

(def interz [(-->> 1 "a" "b")
             (-->> 2 "b" "c")
             (-->> 3 "c" "d")
             (choice [(-->> 4 "1" "2") (-->> 5 "1" "2")]
                     [(-->> 6 "1" "2") (-->> 7 "1" "2")])
             (-->> 8 "v" "b")
             (-->> 9 "v" "b")
             (rec :test
                  (-->> 11 "1" "2")
                  (-->> 12 "1" "3")
                  (continue :test)
                  )
             (-->> "sdasd" "1" "2")])
;
(link-interactions-by-reference 1 interz)