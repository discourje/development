;monitoring.clj
(in-ns 'discourje.core.async)


;load helper namespace files!
;(load "validation/closevalidation"
;      "validation/receivevalidation"
;      "validation/sendvalidation")

(declare contains-value? is-valid-interaction?)

(defn equal-monitors?
  "Check if all channels have the same monitor"
  [channels]
  (= 1 (count (distinct (for [c channels] (get-monitor-id (get-monitor c)))))))


(defn force-monitor-reset! "Force the monitor to go back to the first interaction." [monitor interactions]
  (reset! (:active-interaction monitor) interactions))

(defrecord monitor [id active-interaction recursion-set]
  monitoring
  (get-monitor-id [this] id)
  (get-active-interaction [this] @active-interaction)
  (apply-send! [this target-interaction pre-swap-interaction sender receivers message] (apply-sendable! target-interaction pre-swap-interaction active-interaction this sender receivers message))
  (apply-receive! [this target-interaction pre-swap-interaction sender receivers message] (apply-receivable! target-interaction pre-swap-interaction active-interaction this sender receivers message))
  (valid-send? [this sender receivers message] (let [pre-swap @active-interaction]
                                                 (->swappable-interaction pre-swap (is-valid-sendable? pre-swap this sender receivers message))))
  (valid-receive? [this sender receivers message] (let [pre-swap @active-interaction]
                                                    (->swappable-interaction pre-swap (is-valid-receivable? pre-swap this sender receivers message))))
  (is-current-multicast? [this message] (is-multicast? @active-interaction this message))
  (get-rec [this name] (name @recursion-set))
  (valid-close? [this sender receiver] (let [pre-swap @active-interaction]
                                         (->swappable-interaction pre-swap (is-valid-closable? pre-swap this sender receiver))))
  (apply-close! [this target-interaction pre-swap-interaction channel] (apply-closable! target-interaction pre-swap-interaction active-interaction this channel)))