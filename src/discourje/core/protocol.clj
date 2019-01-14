(ns discourje.core.protocol
  (require [discourje.core.protocolCore :refer :all])
  (use [discourje.core.monitor :only [activateNextMonitor]]
       [discourje.core.validator :only [log-error]]))

(defn generateProtocol
  "Generate the protocol, channels and set the first monitor active"
  [monitors]
  (if (isProtocolValid? monitors)
    (let [protocol (->protocolInstance (generateChannels (getDistinctParticipants monitors)) (atom monitors) (atom nil) monitors)]
      (activateNextMonitor protocol)
      (atom protocol))
    (log-error :invalid-protocol "Supplied monitors are invalid! Make sure there are no duplicate monitor-recursion and that they are recurred and ended correctly!")))
