;protocols.clj
(in-ns 'discourje.core.async)

(defprotocol monitoring
  (get-monitor-id [this])
  (get-active-interaction [this])
  (apply-receive! [this target-interaction pre-swap-interaction sender receivers message])
  (apply-send! [this target-interaction pre-swap-interaction sender receivers message])
  (valid-send? [this sender receivers message])
  (valid-receive? [this sender receivers message])
  (valid-close? [this sender receiver])
  (apply-close! [this target-interaction pre-swap-interaction channel])
  (is-current-multicast? [this message])
  (get-rec [this name]))

(defprotocol protocolable
  (get-interactions [this]))

(defrecord protocol [interactions]
  protocolable
  (get-interactions [this] interactions))

(defprotocol transportable
  (get-provider [this])
  (get-consumer [this])
  (get-chan [this])
  (get-monitor [this])
  (get-buffer [this]))

(defprotocol mappable-rec
  (get-rec-name [this])
  (get-mapping [this])
  (get-mapped-rec [this]))

(defn apply-mapping-to-rec [rec mapping]
  (if (nil? mapping)
    rec
    rec))

(defrecord rec-table-entry [name mapping rec]
  mappable-rec
  (get-rec-name [this] name)
  (get-mapping [this] mapping)
  (get-mapped-rec [this] (apply-mapping-to-rec rec mapping)))