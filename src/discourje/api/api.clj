(ns discourje.api.api
  (require [discourje.core.monitor :refer :all]
           [discourje.core.core :refer :all]
           [discourje.core.protocol :refer :all]
           [discourje.core.protocolCore :refer :all]
           [discourje.core.dataStructures :refer :all]
           [discourje.core.validator :refer :all]
           [clj-uuid :as uuid]))

(defn monitor-send
  "make send monitor, sending action from sender to receiver."
  [action sender receiver]
  (->sendM (uuid/v1) action sender receiver))

(defn monitor-receive
  "make receive monitor, listening for action on receiver send by sender."
  [action receiver sender]
  (->receiveM (uuid/v1) action receiver sender))

(defn monitor-recursion
  "make recursion monitor, which will recur or end after encountering recur record."
  [name protocol]
  (->recursion (uuid/v1) name protocol))

(defn monitor-choice
  "make choice monitor, allowing to observe both first monitors in branches"
  [trueBranch falseBranch]
  (->choice (uuid/v1) trueBranch falseBranch))

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
  ([action value sender receiver]
  (send-to sender action value receiver))
  ([action value sender receiver callback]
   (send-to-> sender action value receiver callback)))

(defmacro s!
  "send macro"
  [action value sender receiver]
  `(send-to ~sender ~action ~value ~receiver))

(defmacro >s!
  "fn [x] value into send! chained macro"
  ([action function sender receiver]
   `(fn [~'callback-value-for-fn]
      (send-to ~sender ~action (~function ~'callback-value-for-fn) ~receiver))))

(defmacro s!>
  "Send! and invoke function-after-send"
  [action value sender receiver function-after-send]
  `(do ~`(send-to ~sender ~action ~value ~receiver)
       ~function-after-send))

(defmacro >s!>
  "fn [x] value into send! and invoke function-after-send chained macro"
  ([action function sender receiver function-after-send]
   `(fn [~'callback-value]
      `(do
         ~(send-to ~sender ~action (~function ~'callback-value) ~receiver)
      ~~function-after-send))))

(defmacro s!->
  "send macro which also invokes callback if the put value is taken"
  [action value sender receiver callback]
   `(send-to-> ~sender ~action ~value ~receiver `(fn [~'callback-value-for-fn] (~~callback))))

(defmacro >s!->
  "fn [x] value into send! and invoke function-after-send only when the put is taken chained macro"
  ([action function sender receiver function-after-send]
   `(fn [~'callback-value]
         ~(send-to-> ~sender ~action (~function ~'callback-value) ~receiver ~function-after-send))))

(defn recv!
  "receive action from sender on receiver, invoking callback"
  [action sender receiver callback]
  (receive-by receiver action sender callback))

(defmacro r!
  "receive macro"
  [action sender receiver callback]
  `(receive-by ~receiver ~action ~sender ~callback))

(defn set-monitor-logging
  "Set logging level to messages only, continuing communication when invalid communication occurs"
  []
  (set-logging))

(defn set-monitor-exceptions
  "Set logging level to exceptions, blocking communication when invalid communication occurs."
  []
  (set-logging-exceptions))

(defn log
  "Log a message to the logging channel"
  [message & more]
  (discourje.core.validator/log-message message more))

(defn log-exception
  "Log a message to the logging channel"
  [type message & more]
  (discourje.core.validator/log-error type message more))

(defn close-logging
  "close the logging channel"
  []
  (discourje.core.validator/stop-logging))

