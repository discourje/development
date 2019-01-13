(ns discourje.core.monitor
  (:require [clojure.core.async]
            [discourje.core.dataStructures :refer :all])
  (use [discourje.core.protocolCore :only [findRecurByName]])
  (:import (clojure.lang Seqable)
           (discourje.core.dataStructures choice sendM recur! receiveM recursion)))

(defn generateRecur
  "generate recursion"
  [name]
  (->recur! name :recur))

(defn generateRecurStop
  "generate end recursion"
  [name]
  (->recur! name :end))

(defn- resetMonitor!
  "Reset! the monitor ATOM"
  ([nextMonitor protocol subvecIndex]
   (reset! (:protocol @protocol) (subvec @(:protocol @protocol) subvecIndex))
   (reset! (:activeMonitor @protocol) nextMonitor))
  ([nextMonitor protocol recursionProtocol subvecIndex]
   (reset! (:protocol @protocol) (subvec recursionProtocol subvecIndex))
   (reset! (:activeMonitor @protocol) nextMonitor))
  ([protocol]
   (reset! (:activeMonitor @protocol) nil)))

(defn activateChoiceBranch
  "activates the choice branch and filters out the branch which was not chosen"
  [protocol branch]
  ;(reset! (:protocol @protocol) (subvec (vec (mapcat identity [branch @(:protocol @protocol)])) 2)) ;todo check if long enough to select index 1!!!
  (if (= 1 (count branch))
    (if (> (count @(:protocol @protocol)) 0)
      (let [nextMonitor (first @(:protocol @protocol))]
        ;(println "next monitor =  " nextMonitor)
        (resetMonitor! nextMonitor protocol 1))
      (resetMonitor! protocol))
    (do
      (reset! (:protocol @protocol) (subvec (vec (mapcat identity [branch @(:protocol @protocol)])) 2)) ;todo check if long enough to select index 1!!!
      (reset! (:activeMonitor @protocol) (nth branch 1))
        (println "next monitor IN CHOICE is " (nth branch 1))
      ))
  )

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
        (instance? sendM currentMonitor)
        (reset! (:activeMonitor @protocol) (->sendM (:action currentMonitor) (:from currentMonitor) newRecv))
        (instance? receiveM currentMonitor)
        (reset! (:activeMonitor @protocol) (->receiveM (:action currentMonitor) newRecv (:from currentMonitor)))
        ))))

(defn monitorValid?
  "is the current monitor valid, compared the current monitor's action, from and to to the given values"
  ([activeM action from to]
   (and
     (and (if (instance? Seqable action)
            (or (contains-value? (:action activeM) action) (= action (:action activeM)))
            (or (= action (:action activeM)) (contains-value? action (:action activeM)))))
     (monitorValid? activeM from to)))
  ([activeM from to]
   (and
     (= from (:from activeM))
     (and (if (instance? Seqable (:to activeM))
            (or (contains-value? to (:to activeM)) (= to (:to activeM)))
            (or (= to (:to activeM)) (contains-value? (:to activeM) to)))))))

(defn canCloseProtocol?
  "can all channels of the protocol be closed?"
  [protocol]
  (= (count @(:protocol @protocol)) 0))

(defn closeProtocol!
  "Close all channels of the protocol"
  [protocol]
  (when (canCloseProtocol? protocol)
    (let [channels (:channels @protocol)]
      (doseq [chan channels]
        ; (clojure.core.async/close! (:channel chan))
        )))
  )


(defn monitorsEqual?
  "Are monitor a and b equal?
  Checked by equal action-from-to"
  [monitor-a monitor-b]
  (let [a monitor-a
        b @monitor-b]
    ;(println "a = " (type a))
    ;(println "b = " (type b))
    (and
      (= (:to a) (:to b))
      (= (:from a) (:from b))
      (= (:action a) (:action b))
      (= (type a) (type b)))))

(defn activateNextMonitor                                   ;todo type of monitor to compare recv to send!
  "Set the active monitor based on the protocol"
  ([action from to protocol]
   (let [activeM @(:activeMonitor @protocol)]
     (cond
       (or (instance? receiveM activeM) (instance? sendM activeM))
       (let [nextMonitor (first @(:protocol @protocol))]
         (cond
           (instance? recursion nextMonitor)
           (let [firstRecMonitor (first (:protocol nextMonitor))
                 recursionProtocol (:protocol nextMonitor)]
             (resetMonitor! firstRecMonitor protocol recursionProtocol 1))
           (instance? recur! nextMonitor)
           (if (= :recur (:status nextMonitor))
             (let [recursive (discourje.core.protocolCore/findRecurByName (:template @protocol) (:name nextMonitor))
                   firstRecMonitor (first (:protocol recursive))
                   recursionProtocol (:protocol recursive)]
               (resetMonitor! firstRecMonitor protocol recursionProtocol 1))
             (when (> (count @(:protocol @protocol)) 1)
               (let [secondNextMonitor (nth @(:protocol @protocol) 1)]
                 (resetMonitor! secondNextMonitor protocol 2))
               ;(closeProtocol! protocol)
               ))
           :else
           (if (> (count @(:protocol @protocol)) 0)
             (do
               ;(println "next monitor =  " nextMonitor)
               (resetMonitor! nextMonitor protocol 1))
             (resetMonitor! protocol)
             ))
         )
       (instance? choice activeM)
       (let [trueResult (monitorValid? (first (:trueBranch activeM)) action from to)
             falseResult (monitorValid? (first (:falseBranch activeM)) action from to)]
         (cond
           ;(and
           trueResult
           ;(not= (monitorsEqual? (first (:falseBranch activeM)) (:activeMonitor @protocol)))                )
           ;(do
             ;(println "Taking TrueBranch")
             (activateChoiceBranch protocol (:trueBranch activeM))
             ;)
           ;(and
           falseResult
           ;(not= (monitorsEqual? (first (:trueBranch activeM)) (:activeMonitor @protocol)))                )
           ;(do
             ;(println "Taking falseBranch")
             (activateChoiceBranch protocol (:falseBranch activeM))
             ;)
           )))))
  ([protocol]
   (let [nextMonitor (first @(:protocol protocol))]
     (if (instance? recursion nextMonitor)
       (let [firstRecMonitor (first (:protocol nextMonitor))
             recursionProt (:protocol nextMonitor)]
         (reset! (:protocol protocol) (subvec recursionProt 1))
         (reset! (:activeMonitor protocol) firstRecMonitor)
         )
       (do
         (reset! (:protocol protocol) (subvec @(:protocol protocol) 1))
         (reset! (:activeMonitor protocol) nextMonitor))))))


(defn activateMonitorOnSend
  "activate a new monitor when specific sendM is encountered"
  [action from to protocol]
  (let [activeM @(:activeMonitor @protocol)]
    (when (or (instance? sendM activeM) (instance? choice activeM))
      (activateNextMonitor action from to protocol))))

(defn isCommunicationValid?
  "Checks if communication is valid by comparing input to the active monitor"
  [action from to protocol]
  (let [activeM @(:activeMonitor @protocol)]
    (cond
      (or (instance? sendM activeM) (instance? receiveM activeM))
      (monitorValid? activeM action from to)
      (instance? choice activeM)
      (let [trueResult (monitorValid? (first (:trueBranch activeM)) action from to)
            falseResult (monitorValid? (first (:falseBranch activeM)) action from to)]
        ;(cond trueResult (activateChoiceBranch protocol (:trueBranch activeM)) ;todo here the choice gets set !!! this should not happen i think
        ;      falseResult (activateChoiceBranch protocol (:falseBranch activeM)))
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