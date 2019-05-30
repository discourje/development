;referencedInteractionLinking.clj
(in-ns 'discourje.core.async)

(def interz [(-->> 1 "a" "b")
             (-->> 2 "b" "c")
             (-->> 3 "c" "d")])

(defn- assoc-next-atom "assoc the inter to the :next key of the previous" [prev inter]
  (assoc prev :next inter))

(defn link-interactions=by-reference [interactions]
  (let [first-interaction (atom (first interactions))]
    (if (< 1 (count interactions))
      (loop [i 1
             new-inter first-interaction]
        (let [inter (atom (nth interactions i))]
          (cond
            (satisfies? interactable (nth interactions i))
            (do (swap! new-inter assoc-next-atom inter)
                (when (true? (< i (- (count interactions) 1)))
                  (recur (+ 1 i) inter)))
            :else
            (log-error :unsupported-reference-link "unsupported reference link interaction"))))
      first-interaction)))

(println (link-interactions=by-reference interz))