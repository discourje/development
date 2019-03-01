(ns discourje.core.async.async
  (:require [clojure.core.async :as async :exclude [>!! <!!]]
            [clj-uuid :as uuid]
            [discourje.core.async.logging :refer :all])
  ;(:refer [clojure.core.async :rename {>!! core>!!, <!! core<!!}])
  (:import (clojure.lang Seqable)))

;load helper namespace files!
(load "interactions")
(load "channels")
;(load "fsm")
(load "monitoring")

(defprotocol sendable
  (get-label [this])
  (get-content [this]))

(defprotocol protocolable
  (get-interactions [this]))

(defrecord message [label content]
  sendable
  (get-label [this] label)
  (get-content [this] content))

(defrecord protocol [interactions]
  protocolable
  (get-interactions [this] interactions))

(defn -->> [action sender receiver]
  "Creates an interaction object specifying sending action from sender to receiver."
  (->interaction (uuid/v1) action sender receiver nil))

(defn make-choice
  "Create a choice interaction"
  [branches]
  (->choice (uuid/v1) branches nil))

(defn make-recursion
  "Generate recursion"
  [name recursion]
  (->recursion (uuid/v1) name recursion nil))

(defn end-recur
  "end a recursion"
  [name]
  (->recur-identifier (uuid/v1) name :end nil))

(defn do-recur
  "do recur to start of recursion"
  [name]
  (->recur-identifier (uuid/v1) name :recur nil))

(defn create-protocol [interactions]
  "Generate protocol based on interactions"
  (->protocol interactions))

(defn replace-last-in-vec
  "Replace the last value in a vector and return the new vector."
  [coll x]
  (conj (pop coll) x))

(defn assoc-next-nested-choice
  "Assoc nested choice next field recursively"
  [linked-i inter]
  (assoc linked-i :branches
                  (vec
                    (for [b (get-branches linked-i)]
                      (if (satisfies? branch (last b))
                        (replace-last-in-vec b (assoc-next-nested-choice (last b) inter))
                        (replace-last-in-vec b (assoc (last b) :next (get-id inter))))))))

(defn assoc-next-nested-do-recur[])
(defn assoc-next-nested-end-recur[])

(defn- link-interactions
  ([protocol]
   (let [interactions (get-interactions protocol)
         helper-vec (atom [])
         linked-interactions (atom [])]
     (link-interactions interactions helper-vec linked-interactions)))
  ([interactions helper-vec linked-interactions]
   (do (doseq [inter interactions]
         (cond
           (satisfies? recursable inter)
           (let [recured-interactions
                 (for [recursion (get-recursion inter)]
                   (let [recursion-help-vec (atom [])
                         linked-recursion-interactions (atom [])]
                     (link-interactions recursion recursion-help-vec linked-recursion-interactions)))
                 last-helper-mon (last @helper-vec)
                 linked-i (if (nil? last-helper-mon) nil (assoc last-helper-mon :next (get-id inter)))
                 new-recursion (->recursion (get-id inter) (get-name inter) recured-interactions nil)]
             (swap! helper-vec conj new-recursion)
             (when-not (nil? last-helper-mon)
               (if  (satisfies? branch linked-i)
                 (swap! linked-interactions conj (assoc-next-nested-choice linked-i inter));(assoc linked-i :branches (vec (for [b (get-branches linked-i)] (replace-last-in-vec b (assoc (last b) :next (get-id inter)))))))
                 (swap! linked-interactions conj linked-i)))
             )
           (satisfies? branch inter)
               (let [branched-interactions
                     (for [branch (get-branches inter)]
                       (let [branch-help-vec (atom [])
                             linked-branch-interactions (atom [])]
                         (link-interactions branch branch-help-vec linked-branch-interactions)))
                     last-helper-mon (last @helper-vec)
                     linked-i (if (nil? last-helper-mon) nil (assoc last-helper-mon :next (get-id inter)))
                     new-choice (->choice (get-id inter) branched-interactions nil)]
                 (swap! helper-vec conj new-choice)
                 (when-not (nil? last-helper-mon)
                 (if  (satisfies? branch linked-i)
                   (swap! linked-interactions conj (assoc-next-nested-choice linked-i inter));(assoc linked-i :branches (vec (for [b (get-branches linked-i)] (replace-last-in-vec b (assoc (last b) :next (get-id inter)))))))
                   (swap! linked-interactions conj linked-i)))
                 )
           (empty? @helper-vec) (swap! helper-vec conj inter)
           (or (satisfies? interactable inter)(satisfies? identifiable-recur inter)) (let [i (last @helper-vec)
                                                 linked-i (assoc i :next (get-id inter))]
                                             (swap! helper-vec conj inter)
                                             (if (satisfies? branch linked-i)
                                               (swap! linked-interactions conj (assoc-next-nested-choice linked-i inter));)(assoc linked-i :branches (vec (for [b (get-branches linked-i)] (replace-last-in-vec b (assoc (last b) :next (get-id inter)))))))
                                               (swap! linked-interactions conj linked-i)))))
       (swap! linked-interactions conj (last @helper-vec)))
   @linked-interactions))

(defn generate-monitor
  "Generate the monitor based on the given protocol"
  [protocol]
  (let [linked-interactions (link-interactions protocol)]
    (->monitor (uuid/v1) linked-interactions (atom (first linked-interactions)))))

(defn generate-infrastructure
  "Generate channels with monitor based on the protocol"
  [protocol]
  (let [monitor (generate-monitor protocol)
        roles (get-distinct-roles (get-interactions protocol))
        channels (generate-channels roles monitor 1)]
    channels))

(defn- allow-send
  "Allow send message in channel"
  [channel message]
  (log-message "allowing send on channel!")
  (async/>!! (get-chan channel) message))

(defn- allow-receive [channel]
  (log-message "allowing receive on channel!")
  (async/<!! (get-chan channel)))

(defn- allow-sends
  "Allow sending message on multiple channels"
  [channels message]
  (doseq [c channels] (allow-send c message)))

(defn all-valid-channels?
  "Do all channels comply with the monitor"
  [channels message]
  (= 1 (count (distinct (for [c channels] (valid-interaction? (get-monitor c) (get-provider c) (get-consumer c) (get-label message)))))))

(defn >!!!
  "Put on channel"
  [channel message]
  ;  (if (nil? (get-active-interaction (get-monitor channel)))
  ;   (println "Please activate a monitor, your protocol has not yet started, or it is already finished!")
  (if (vector? channel)
    (do (when-not (equal-senders? channel)
          (log-error :invalid-parallel-channels "Trying to send in parallel, but the sender of the channels is not the same!"))
        (when-not (equal-monitors? channel)
          (log-error :monitor-mismatch "Trying to send in parallel, but the channels do not share the same monitor!"))
        (when-not (all-valid-channels? channel message)
          (log-error :incorrect-communication "Trying to send in parallel, but the monitor is not correct for all of them!"))
        (let [monitor (get-monitor (first channel))]
          ;(apply-interaction monitor (get-label message))
          (allow-sends channel message)))
    (do (when-not (valid-interaction? (get-monitor channel) (get-provider channel) (get-consumer channel) (get-label message))
          (log-error :incorrect-communication  "Atomic-send communication invalid!"))
        ; (apply-interaction (get-monitor channel) (get-label message))
        (allow-send channel message))))
;)

(defn <!!!
  "Take from channel"
  [channel label]
  (if (nil? (get-active-interaction (get-monitor channel)))
    (log-error :invalid-monitor "Please activate a monitor, your protocol has not yet started, or it is already finished!")
    (let [result (allow-receive channel)]
      (do (when-not (valid-interaction? (get-monitor channel) (get-provider channel) (get-consumer channel) label)
            (log-error :incorrect-communication "Atomic receive communication invalid!"))
          (apply-interaction (get-monitor channel) (get-provider channel) (get-consumer channel) label)
          result))))


;(defn get-transitions-in-protocol [protocol]
;  (interactions-to-transitions (get-interactions protocol)))
;
;(defn- generate-io-fsms
;  "Convert a protocol of interactions to IO enabled finite-state-machines local to each role."
;  [protocol]
;  (let [roles (get-distinct-roles (get-interactions protocol))]))

