(ns discourje.api.api
  (require [discourje.core.monitor :refer :all]
           [discourje.core.protocol :refer :all]
           [discourje.core.protocolCore :refer :all]
           [discourje.core.dataStructures :refer :all]))

(defn monitor-send
  "make send monitor, sending action from sender to receiver."
  [action sender receiver]
  (->sendM action sender receiver))

(defn monitor-receive
  "make receive monitor, listening for action on receiver send by sender."
  [action sender receiver]
  (->receiveM action receiver sender))

(defn generateProtocolFromMonitors
  "generate the protocol, channels and set the first monitor active"
  [monitors]
  (generateProtocol monitors))

(defn send!
  "send action with value from sender to receiver"
  [action value sender receiver]
  (send-to sender action value receiver))

(defn recv!
  "receive action from sender on receiver, invoking callback"
  [action sender receiver callback]
  (receive-by receiver action sender callback))