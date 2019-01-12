(ns discourje.core.protocol
  (require [discourje.core.protocolCore :refer :all])
  (use [discourje.core.monitor :only [activateNextMonitor]]))

(defn generateProtocol
  "generate the protocol, channels and set the first monitor active"
  [monitors]
  (when (isProtocolValid? monitors)
    (let [protocol (->protocolInstance (generateChannels (getDistinctParticipants monitors)) (atom monitors) (atom nil) monitors)]
      (activateNextMonitor protocol)
      protocol)))
