(ns discourje.api.api
  (require [discourje.core.monitor :refer :all]
                              [discourje.core.protocol :refer :all]
                              [discourje.core.dataStructures :refer :all]))

(defn monitor-send
  "make send monitor, sending action from sender to receiver."
  [action sender receiver]
  (->sendM action sender receiver))

(defn monitor-receive
  "make receive monitor, listening for action on receiver send by sender."
  [action sender receiver]
  (->receiveM action receiver sender))