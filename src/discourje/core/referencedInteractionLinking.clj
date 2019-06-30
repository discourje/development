;referencedInteractionLinking.clj
(in-ns 'discourje.core.async)

(defn- assoc-next-atom "assoc the inter to the :next key of the previous" [prev inter]
  (assoc prev :next inter))

(defn link-interactions-by-reference [protocol]
  (let [interactions (get-interactions protocol)
        first-interaction (atom (first interactions))]
    (when (< 1 (count interactions))
      (loop [i 1
             new-inter first-interaction]
        (let [inter (atom (nth interactions i))]
          (cond
            (satisfies? interactable (nth interactions i))
            (do (swap! new-inter assoc-next-atom inter)
                (when (true? (< i (- (count interactions) 1)))
                  (recur (+ 1 i) inter)))
            :else
            (log-error :unsupported-reference-link "unsupported reference link interaction")))))
    first-interaction))

;(def interz [(-->> 1 "a" "b")
;             (-->> 2 "b" "c")
;             (-->> 3 "c" "d")])
;
;(println (link-interactions=by-reference interz))