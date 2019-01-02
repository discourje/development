(ns discourje.core.protocol
  (require [discourje.core.protocolCore :refer :all])
  (use [discourje.core.monitor :only [activateNextMonitor]]))

(defn generateProtocol
  "generate the protocol, channels and set the first monitor active"
  [roles monitors]
  (when (isProtocolValid? monitors)
    (let [protocol (->protocolInstance (generateChannels roles) (atom monitors) (atom nil) monitors)]
      (activateNextMonitor protocol)
      protocol)))
