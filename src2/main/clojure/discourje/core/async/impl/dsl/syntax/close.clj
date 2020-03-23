;close construct
(in-ns 'discourje.core.async.impl.dsl.syntax)
;;---------------------------------Linkable implementation-------------------------------------------------
(defn- apply-rec-mapping-closer! [this mapping]
  (if (nil? (get-next this))
    (assoc (assoc this :sender (map-value! (get-from this) mapping)) :receiver (map-value! (get-to this) mapping))
    (assoc (assoc (assoc this :sender (map-value! (get-from this) mapping)) :receiver (map-value! (get-to this) mapping)) :next (apply-rec-mapping (get-next this) mapping))
    )
  )