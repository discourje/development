(ns discourje.core.monitor
  (:require [clojure.core.async])
  (:import (clojure.lang Seqable Atom)))

;Define a monitor to check communication, this will be used to verify correct conversation.
;This is just a data structure to group related information.
;This monitor `embeds' a send! and recv! pair, meaning the monitor will only complete when the receiving end was successful
(defrecord monitor [action from to])
;monitor for a single send action
(defrecord sendM [action from to])
;monitor for a single recv action
(defrecord receiveM [action to from])
;We also need a data structure to create a conditional with branches.
;When the protocol encounters this it will check the conditional and continue on the correct branch.
(defrecord choice [trueBranch falseBranch])
;recursion construct
(defrecord recursion [name protocol])
;recur or end the recursion block
(defrecord recur! [name status])

(defn generateRecur
  "generate recursion"
  [name]
  (->recur! name :recur))

(defn generateRecurStop
  "generate end recursion"
  [name]
  (->recur! name :end))

(defn activateChoiceBranch
  "activates the choice branch and filters out the branch which was not chosen"
  [protocol branch]
  (reset! (:activeMonitor @protocol) (first branch))
  (reset! (:protocol @protocol) (subvec (vec (mapcat identity [branch @(:protocol @protocol)])) 1)))

(defn- findNestedRecurByName
  "Find a (nested) recursion map in the protocol by name, preserves nested structure in result!"
  [protocol name]
  (for [element protocol
        :when (or (instance? recursion element) (instance? choice element))]
    (cond
      (instance? recursion element)
      (if (= (:name element) name)
        element
        (findNestedRecurByName (:protocol element) name))
      (instance? choice element)
      (let [trueResult (findNestedRecurByName (:trueBranch element) name)
            falseResult (findNestedRecurByName (:falseBranch element) name)]
        (if (not (nil? trueResult))
          trueResult
          falseResult)))))

(defn findRecurByName
  "Find a (nested) recursion map in the protocol, returns the recursion map directly!"
  [protocol name]
  (println name)
  (let [x (findNestedRecurByName protocol name)]
    (first (drop-while empty? (flatten x)))))

(defn incorrectCommunication
  "communication incorrect, log a message! (or maybe throw exception)"
  [message]
  (println message))

(defn contains-value?
  "Does the vector contain a value?"
  [element coll]
  (boolean (some #(= element %) coll)))

(defn hasMultipleReceivers?
  "Check if the :to key of the active monitor is a Seqable(collection) and if there are more than 1 receivers"
  [protocol]
  (and
    (instance? Seqable (:to @(:activeMonitor @protocol)))
    (> (count (:to @(:activeMonitor @protocol))) 1)))

(defn removeReceiver
  "Remove a receiver from the monitor if there are multiple"
  [protocol to]
  (when (hasMultipleReceivers? protocol)
    (let [currentMonitor @(:activeMonitor @protocol)
          recv (:to currentMonitor)
          newRecv (vec (remove #{to} recv))]
      (cond
        (instance? monitor currentMonitor)
        (reset! (:activeMonitor @protocol) (->monitor (:action currentMonitor) (:from currentMonitor) newRecv))
        (instance? sendM currentMonitor)
        (reset! (:activeMonitor @protocol) (->sendM (:action currentMonitor) (:from currentMonitor) newRecv))
        (instance? receiveM currentMonitor)
        (reset! (:activeMonitor @protocol) (->receiveM (:action currentMonitor) newRecv (:from currentMonitor)))
        ))))

(defn monitorValid?
  "is the current monitor valid, compared the current monitor's action, from and to to the given values"
  [activeM action from to]
  (println "+++++++++")
  (println (format "action = %s || activeM action = %s result : %s" action (:action activeM)(and (if (instance? Seqable action)
                                                                                                   (or (contains-value? (:action activeM) action) (= action (:action activeM)))
                                                                                                   (or (= action (:action activeM)) (contains-value? action (:action activeM)))))))
  (println (format "from = %s || activeM from = %s result: %s" from (:from activeM)  (= from (:from activeM))))
  (println (format "to = %s || activeM TO = %s result: %s" to (:to activeM)(and (if (instance? Seqable (:to activeM))
                                                                                  (or (contains-value? to (:to activeM)) (= to (:to activeM)))
                                                                                  (or (= to (:to activeM)) (contains-value? (:to activeM) to))))))
  (println "+++++++++")
  (and
    (and (if (instance? Seqable action)
           (or (contains-value? (:action activeM) action) (= action (:action activeM)))
           (or (= action (:action activeM)) (contains-value? action (:action activeM)))))
    (= from (:from activeM))
    (and (if (instance? Seqable (:to activeM))
           (or (contains-value? to (:to activeM)) (= to (:to activeM)))
           (or (= to (:to activeM)) (contains-value? (:to activeM) to))))))

(defn canCloseProtocol?
  "can all channels of the protocol be closed?"
  [protocol]
  (= (count @(:protocol @protocol)) 1))


(defn closeProtocol!
  "Close all channels of the protocol"
  [protocol]
  (when (canCloseProtocol? protocol)
    (let [channels (:channels @protocol)]
      (doseq [chan channels]
        ;(clojure.core.async/close! (:channel chan))
        )))
  )
(defn- resetMonitor!
  "Reset! the monitor ATOM"
  ([nextMonitor protocol subvecIndex]
   (reset! (:activeMonitor @protocol) nextMonitor)
   (reset! (:protocol @protocol) (subvec @(:protocol @protocol) subvecIndex)))
  ([nextMonitor protocol recursionProtocol subvecIndex]
   (reset! (:activeMonitor @protocol) nextMonitor)
   (reset! (:protocol @protocol) (subvec recursionProtocol subvecIndex))))

(defn activateNextMonitor
  "Set the active monitor based on the protocol"
  ([action from to protocol]
   (let [activeM @(:activeMonitor @protocol)]
     (cond
       (or (instance? monitor activeM) (instance? receiveM activeM) (instance? sendM activeM))
       (let [nextMonitor (first @(:protocol @protocol))]
         (cond
           (instance? recursion nextMonitor)
           (let [firstRecMonitor (first (:protocol nextMonitor))
                 recursionProtocol (:protocol nextMonitor)]
             (resetMonitor! firstRecMonitor protocol recursionProtocol 1))
           (instance? recur! nextMonitor)
           (if (= :recur (:status nextMonitor))
             (let [recursive (findRecurByName (:template @protocol) (:name nextMonitor))
                   firstRecMonitor (first (:protocol recursive))
                   recursionProtocol (:protocol recursive)]
               (resetMonitor! firstRecMonitor protocol recursionProtocol 1))
             (when (> (count @(:protocol @protocol)) 1)
               (let [secondNextMonitor (nth @(:protocol @protocol) 1)]
                 (resetMonitor! secondNextMonitor protocol 2))
               ;(closeProtocol! protocol)
               ))
           :else
           (when (> (count @(:protocol @protocol)) 0)
             (resetMonitor! nextMonitor protocol 1)
             ;(closeProtocol! protocol)
             ))
         )
       (instance? choice activeM)
       (let [trueResult (monitorValid? (first (:trueBranch activeM)) action from to)
             falseResult (monitorValid? (first (:falseBranch activeM)) action from to)]
         (cond trueResult (activateChoiceBranch protocol (:trueBranch activeM))
               falseResult (activateChoiceBranch protocol (:falseBranch activeM)))))))
  ([protocol]
   (let [nextMonitor (first @(:protocol protocol))]
     (if (instance? recursion nextMonitor)
       (let [firstRecMonitor (first (:protocol nextMonitor))
             recursionProt (:protocol nextMonitor)]
         (reset! (:activeMonitor protocol) firstRecMonitor)
         (reset! (:protocol protocol) (subvec recursionProt 1))
         )
       (do
         (reset! (:activeMonitor protocol) nextMonitor)
         (reset! (:protocol protocol) (subvec @(:protocol protocol) 1)))))))


(defn activateMonitorOnSend
  "activate a new monitor when specific sendM is encountered"
  [action from to protocol]
  (let [activeM @(:activeMonitor @protocol)]
    (when (instance? sendM activeM))
    (activateNextMonitor action from to protocol)))

(defn isCommunicationValid?
  "Checks if communication is valid by comparing input to the active monitor"
  [action from to protocol]
  (let [activeM @(:activeMonitor @protocol)]
    (cond
      (or (instance? monitor activeM) (instance? sendM activeM) (instance? receiveM activeM))
      (monitorValid? activeM action from to)
      (instance? choice activeM)
      (let [trueResult (monitorValid? (first (:trueBranch activeM)) action from to)
            falseResult (monitorValid? (first (:falseBranch activeM)) action from to)]
        (cond trueResult (activateChoiceBranch protocol (:trueBranch activeM))
              falseResult (activateChoiceBranch protocol (:falseBranch activeM)))
        (or trueResult falseResult)))))

(defn getTargetBranch
  "Get the target branch of a choice construct based on the action, sender and receiver"
  [action from to protocol]
  (let [activeM @(:activeMonitor @protocol)]
    (when
      (instance? choice activeM)
      (cond
        (monitorValid? (first (:trueBranch activeM)) action from to)
        (first (:trueBranch activeM))
        (monitorValid? (first (:falseBranch activeM)) action from to)
        (first (:falseBranch activeM))))))