(ns discourje.api.api
  (require [discourje.core.monitor :refer :all]
           [discourje.core.core :refer :all]
           [discourje.core.protocol :refer :all]
           [discourje.core.protocolCore :refer :all]
           [discourje.core.dataStructures :refer :all]
           [discourje.core.validator :refer :all]
           [clj-uuid :as uuid]))

(defn monitor-send
  "Make send monitor, sending action from sender to receiver."
  [action sender receiver]
  (->sendM (uuid/v1) action sender receiver))

(defn monitor-receive
  "Make receive monitor, listening for action on receiver send by sender."
  [action receiver sender]
  (->receiveM (uuid/v1) action receiver sender))

(defn monitor-recursion
  "Make recursion monitor, which will recur or end after encountering recur record."
  [name protocol]
  (->recursion (uuid/v1) name protocol))

(defn monitor-choice
  "Make choice monitor, allowing to observe both first monitors in branches"
  [trueBranch falseBranch]
  (->choice (uuid/v1) trueBranch falseBranch))

(defn do-recur
  "Recur back to recursion monitor, matching name!"
  [name]
  (generateRecur name))

(defn do-end-recur
  "End monitor, matching name!"
  [name]
  (generateRecurStop name))

(defn generateProtocolFromMonitors
  "Generate the protocol, channels and set the first monitor active"
  [monitors]
  (generateProtocol monitors))

(defn generateParticipant
  "Generate participant record"
  [name protocol]
  (->participant name protocol))

(defn send!
  "Send action with value from sender to receiver"
  ([action value sender receiver]
   (send-to sender action value receiver))
  ([action value sender receiver callback]
   (send-to!! sender action value receiver callback)))

(defmacro s!
  "Send macro"
  [action value sender receiver]
  `(send-to ~sender ~action ~value ~receiver))

(defmacro >s!
  "fn [x] value into send! chained macro"
  ([action function sender receiver]
   `(fn [~'callback-value-for-fn]
      (send-to ~sender ~action (~function ~'callback-value-for-fn) ~receiver))))

(defmacro s!>
  "Send! and invoke function-after-send immediately after send"
  [action value sender receiver function-after-send]
  `(do ~`(send-to ~sender ~action ~value ~receiver)
       ~function-after-send))

(defmacro >s!>
  "fn [x] value into send! and invokes function-after-send immediately after send chained macro"
  ([action function sender receiver function-after-send]
   `(fn [~'callback-value]
      `(do
         ~(send-to ~sender ~action (~function ~'callback-value) ~receiver)
         ~~function-after-send))))

(defmacro s!!#>
  "Send macro which also invokes callback if the put value is taken.
  Generates anonymous function which invokes the callback.
  Simulates blocking functionality by delaying callback!"
  [action value sender receiver callback]
  `(send-to!! ~sender ~action ~value ~receiver (fn [~'callback-value-for-fn] ~callback)))

(defmacro s!!>
  "Send macro which also invokes callback if the put value is taken
  Simulates blocking functionality by delaying callback!"
  [action value sender receiver callback]
  `(send-to!! ~sender ~action ~value ~receiver ~callback))

(defmacro >s!!>
  "fn [x] value into send! and invoke function-after-send only when the put is taken chained macro.
  Simulates blocking functionality by delaying callback!"
  ([action function sender receiver function-after-send]
   `(fn [~'callback-value]
      (send-to!! ~sender ~action (~function ~'callback-value) ~receiver ~function-after-send))))

(defmacro >!!s!!>
  "fn [x] value into when the active monitor has changed state and then send! and invoke function-after-send only when the put is taken chained macro
  Simulates blocking functionality by delaying callback, this will also wait until the active monitor has changed!"
  ([action function sender receiver function-after-send]
   `(fn [~'callback-value]
      (add-watch (:activeMonitor @(:protocol ~sender)) nil
                 (fn [~'key ~'atom ~'old-state ~'new-state]
                   (remove-watch ~'atom nil)
                   (send-to!! ~sender ~action (~function ~'callback-value) ~receiver ~function-after-send))))))

(defn recv!
  "Receive action from sender on receiver, invoking callback"
  [action sender receiver callback]
  (receive-by receiver action sender callback))

(defmacro r!
  "Receive macro"
  [action sender receiver callback]
  `(receive-by ~receiver ~action ~sender ~callback))

(defmacro r!>
  "Receive macro which generates an anonymous function  with 1 parameter which invokes callback with the value of the function"
  [action sender receiver callback]
  `(receive-by ~receiver ~action ~sender
               (fn [~'callback-value] (~callback ~'callback-value))))

(defmacro >r!>
  "fn [x] value into receive! callback chained macro"
  [action sender receiver callback]
  `(fn [~'callback-value]
     (receive-by ~receiver ~action ~sender (~callback ~'callback-value))))

(defmacro >r!
  "fn [x] value to catch the callback value, and directly invoke callback receive! chained macro"
  [action sender receiver callback]
  `(fn [~'callback-value]
     (receive-by ~receiver ~action ~sender ~callback)))

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
  "Close the logging channel"
  []
  (discourje.core.validator/stop-logging))

