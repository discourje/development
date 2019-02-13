(in-ns 'discourje.core.async.async)

(defprotocol monitoring
  (get-active-interaction [this])
  (apply-interaction [this label]))

(defn- check-atomic-interaction [label active-interaction]
  (= (get-action @active-interaction) label))

;(defn- get-next-interaction [active-interaction interactions]
;  (doseq [interaction interactions]
;    (cond
;      ;(instance? recursion element)
;      ;(flatten (vec (conj result2 (findAllParticipants (:protocol element) result2))))
;      ;(instance? choice element)
;      ;(let [trueResult (findAllParticipants (:trueBranch element) result2)
;      ;      falseResult (findAllParticipants (:falseBranch element) result2)]
;      ;  (if (not (nil? trueResult))
;      ;    (flatten (vec (conj result2 trueResult)))
;      ;    (flatten (vec (conj result2 falseResult)))))
;      (satisfies? discourje.core.async.async/interactable interaction)
;      (w)
;      (do
;        (if (instance? Seqable (get-receivers element))
;          (conj result2 (flatten (get-receivers element)) (get-sender element))
;          (conj result2 (get-receivers element) (get-sender element)))))))

;(defn- swap-active-interaction-by-atomic [active-interaction interactions]
;  (swap! active-interaction (get-next-interaction active-interaction interactions)))

;(defn- apply-interaction [label active-interaction interactions]
;  (cond
;    (and (instance? interaction @active-interaction) (check-atomic-interaction label active-interaction))
;    (swap-active-interaction-by-atomic active-interaction interactions)
;    ))

(defrecord monitor [interactions channels active-interaction]
  monitoring
  (get-active-interaction [this] @active-interaction)
  (apply-interaction [this label])); (apply-interaction label active-interaction interactions)))

