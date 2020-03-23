;branch construct
(in-ns 'discourje.core.async.impl.dsl.syntax)
;;---------------------------------Linkable implementation-------------------------------------------------
(defn- apply-rec-mapping-branch! [this mapping]
  (if (nil? (get-next this))
    (assoc this :branches (for [b (get-branches this)] (apply-rec-mapping b mapping)))
    (assoc (assoc this :branches (for [b (get-branches this)] (apply-rec-mapping b mapping))) :next (apply-rec-mapping (get-next this) mapping))
    )
  )