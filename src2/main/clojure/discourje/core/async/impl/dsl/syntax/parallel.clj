;parallel construct
(in-ns 'discourje.core.async.impl.dsl.syntax)

;;---------------------------------Linkable implementation-------------------------------------------------
(defn- apply-rec-mapping-parallel! [this mapping]
  (if (nil? (get-next this))
    (assoc this :parallels (for [b (get-parallel this)] (apply-rec-mapping b mapping)))
    (assoc (assoc this :parallels (for [b (get-parallel this)] (apply-rec-mapping b mapping))) :next (apply-rec-mapping (get-next this) mapping))
    )
  )