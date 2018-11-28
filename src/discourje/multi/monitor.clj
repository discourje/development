(ns discourje.multi.monitor
  (:import (discourje.multi.core monitor choice)
           (clojure.lang Seqable)))

(defn activateNextMonitor
  "Set the active monitor based on the protocol"
  [protocol]
  (let [nextMonitor (first (:protocol @protocol))]
    (reset! (:activeMonitor @protocol) nextMonitor)
    (reset! (:protocol @protocol) (subvec (:protocol @protocol) 1))))

(defn incorrectCommunication
  "communication incorrect, log a message! (or maybe throw exception)"
  [message]
  (println message))

(defn isCommunicationValid?
  "Checks if communication is valid by comparing input to the active monitor"
  [action from to protocol]
  (let [proto @protocol
        activeM (:activeMonitor proto)]
    (cond
      (instance? monitor activeM)
      (do
        (println "yes is monitor")
        (println (:to activeM))
        (and
          (= action (:action activeM))
          (= from (:from activeM))
          (and (if (instance? Seqable (:to activeM))
                 (contains? (:to activeM) to)
                 (= to (:to activeM))))))
      (instance? choice activeM)
      (do
        (println "yes is choice")))))