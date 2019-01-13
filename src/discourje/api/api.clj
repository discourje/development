(ns discourje.api.api
  (require [discourje.core.monitor :refer :all]
           [discourje.core.core :refer :all]
           [discourje.core.protocol :refer :all]
           [discourje.core.protocolCore :refer :all]
           [discourje.core.dataStructures :refer :all]))

(defn monitor-send
  "make send monitor, sending action from sender to receiver."
  [action sender receiver]
  (->sendM action sender receiver))

(defn monitor-receive
  "make receive monitor, listening for action on receiver send by sender."
  [action receiver sender]
  (->receiveM action receiver sender))

(defn monitor-recursion
  "make recursion monitor, which will recur or end after encountering recur record."
  [name protocol]
  (->recursion name protocol))

(defn monitor-choice
  "make choice monitor, allowing to observe both first monitors in branches"
  [trueBranch falseBranch]
  (->choice trueBranch falseBranch))

(defn do-recur
  "recur back to recursion monitor, matching name!"
  [name]
  (generateRecur name))

(defn do-end-recur
  "end monitor, matching name!"
  [name]
  (generateRecurStop name))

(defn generateProtocolFromMonitors
  "generate the protocol, channels and set the first monitor active"
  [monitors]
  (generateProtocol monitors))

(defn generateParticipant
  "generate participant record"
  [name protocol]
  (->participant name protocol))

(defn send!
  "send action with value from sender to receiver"
  [action value sender receiver]
  (send-to sender action value receiver))

(defmacro s!
  "send macro"
  [action value sender receiver]
  `(send-to ~sender ~action ~value ~receiver))

(defn recv!
  "receive action from sender on receiver, invoking callback"
  [action sender receiver callback]
  (receive-by receiver action sender callback))

(defmacro r!
  "receive macro"
  [action sender receiver callback]
  `(receive-by ~receiver ~action ~sender ~callback))



