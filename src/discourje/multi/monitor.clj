(ns discourje.multi.monitor
  (:import (clojure.lang Seqable Atom)))

;Define a monitor to check communication, this will be used to verify correct conversation.
;This is just a data structure to group related information.
(defrecord monitor [action from to])
;We also need a data structure to create a conditional with branches.
;When the protocol encounters this it will check the conditional and continue on the correct branch.
(defrecord choice [trueMonitor falseMonitor trueBranch falseBranch])

(defn activateNextMonitor
  "Set the active monitor based on the protocol"
  [protocol]
  (if (instance? Atom protocol)
    (let [nextMonitor (first @(:protocol @protocol))]
      (reset! (:activeMonitor @protocol) nextMonitor)
      (reset! (:protocol @protocol) (subvec @(:protocol @protocol) 1)))
  (let [nextMonitor (first @(:protocol protocol))]
    (reset! (:activeMonitor protocol) nextMonitor)
    (reset! (:protocol protocol) (subvec @(:protocol protocol) 1)))))

(defn incorrectCommunication
  "communication incorrect, log a message! (or maybe throw exception)"
  [message]
  (println message))

(defn isCommunicationValid?
  "Checks if communication is valid by comparing input to the active monitor"
  [action from to protocol]
  (let [proto @protocol
        activeM @(:activeMonitor proto)]
    (cond
      (instance? monitor activeM)
      (do
        (and
          (= action (:action activeM))
          (= from (:from activeM))
          (and (if (instance? Seqable (:to activeM))
                 (or (contains? (:to activeM) to) (= to (:to activeM)))
                 (= to (:to activeM))))))
      (instance? choice activeM)
      (do
        (println "yes is choice")))))