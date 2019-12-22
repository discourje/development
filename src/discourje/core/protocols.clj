;protocols.clj
(in-ns 'discourje.core.async)

(defprotocol transportable
  (get-provider [this])
  (get-consumer [this])
  (get-chan [this])
  (get-monitor [this])
  (get-buffer [this]))

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
  (get-rec [this name save-mapping]))

(defprotocol protocolable
  (get-interactions [this]))

(defrecord protocol [interactions]
  protocolable
  (get-interactions [this] interactions))