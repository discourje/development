;referencedInteractionLinking.clj
(in-ns 'discourje.core.async)

(def interz [(-->> 1 "a" "b")
             (-->> 1 "b" "c")
             (-->> 1 "c" "d")])

(defn link-interactions-by-reference [interactions]
  (let [linked-interactions (atom [])]
    (doseq [i interactions]
      (let [inter (atom i)]
        (println @inter)
        (cond
          (empty? @linked-interactions)
          (swap! linked-interactions conj inter)
          (satisfies? interactable i)
          (do (swap! (last @linked-interactions) assoc (:next (last @linked-interactions)) inter)
              (swap! linked-interactions conj inter)
              )
          :else
          (log-error :unsupported-reference-link "unsupported reference link interaction")
          )))
    @linked-interactions))

(println (link-interactions-by-reference interz))