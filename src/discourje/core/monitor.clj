(ns discourje.core.monitor
  (:import (clojure.lang Seqable Atom)))

;Define a monitor to check communication, this will be used to verify correct conversation.
;This is just a data structure to group related information.
(defrecord monitor [action from to])
;We also need a data structure to create a conditional with branches.
;When the protocol encounters this it will check the conditional and continue on the correct branch.
(defrecord choice [trueBranch falseBranch])
;recursion construct
(defrecord recursion [name protocol])
;recur the recursion block
(defrecord recur! [name])
;end the recursion block
(defrecord end! [name])

(defn activateChoiceBranch
  "activates the choice branch and filters out the branch which was not chosen"
  [protocol branch]
  (reset! (:activeMonitor @protocol) (first branch))
  (reset! (:protocol @protocol) (subvec (vec (mapcat identity [branch @(:protocol @protocol)])) 1)))

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
          newRecv (vec (remove #{to} recv))
          newMonitor (->monitor (:action currentMonitor) (:from currentMonitor) newRecv)]
      (reset! (:activeMonitor @protocol) newMonitor))))

(defn monitorValid?
  "is the current monitor valid, compared the current monitor's action, from and to to the given values"
  [activeM action from to]
  (and
    (and (if (instance? Seqable action)
           (or (contains-value? (:action activeM) action) (= action (:action activeM)))
           (= action (:action activeM))))
    (= from (:from activeM))
    (and (if (instance? Seqable (:to activeM))
           (or (contains-value? to (:to activeM)) (= to (:to activeM)))
           (= to (:to activeM))))))

(defn activateNextMonitor
  "Set the active monitor based on the protocol"
  ([action from to protocol]
   (let [activeM @(:activeMonitor @protocol)]
     (cond
       (instance? monitor activeM)
       (let [nextMonitor (first @(:protocol @protocol))]
         (when (> (count @(:protocol @protocol)) 0)
           (reset! (:activeMonitor @protocol) nextMonitor)
           (reset! (:protocol @protocol) (subvec @(:protocol @protocol) 1))))
       (instance? choice activeM)
       (let [trueResult (monitorValid? (first (:trueBranch activeM)) action from to)
             falseResult (monitorValid? (first (:falseBranch activeM)) action from to)]
         (cond trueResult (activateChoiceBranch protocol (:trueBranch activeM))
               falseResult (activateChoiceBranch protocol (:falseBranch activeM)))))))
  ([protocol]
   (let [nextMonitor (first @(:protocol protocol))]
     (if (instance? recursion nextMonitor)
         (let [firstRecMonitor (first (:protocol nextMonitor))
             recProt (:protocol nextMonitor)]
         (reset! (:activeMonitor protocol) firstRecMonitor)
         (reset! (:protocol protocol) (subvec recProt 1))
         )
       (do
         (reset! (:activeMonitor protocol) nextMonitor)
         (reset! (:protocol protocol) (subvec @(:protocol protocol) 1)))))))

(defn isCommunicationValid?
  "Checks if communication is valid by comparing input to the active monitor"
  [action from to protocol]
  (let [activeM @(:activeMonitor @protocol)]
    (cond
      (instance? monitor activeM)
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