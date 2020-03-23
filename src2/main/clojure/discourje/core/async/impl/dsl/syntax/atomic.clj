;atomic construct
(in-ns 'discourje.core.async.impl.dsl.syntax)
;;---------------------------------Linkable implementation-------------------------------------------------
(defn- apply-rec-mapping-atomic! [this mapping]
  (if (nil? (get-next this))
    (assoc (assoc this :sender (map-value! (get-sender this) mapping)) :receivers (map-value! (get-receivers this) mapping))
    (assoc (assoc (assoc this :sender (map-value! (get-sender this) mapping)) :receivers (map-value! (get-receivers this) mapping)) :next (apply-rec-mapping (get-next this) mapping))))
