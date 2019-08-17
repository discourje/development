(ns discourje.core.async
  (:require [clj-uuid :as uuid]
            [discourje.core.logging :refer :all]
            [clojure.core.async :as async]
            [clojure.core.async.impl.protocols :as bufs])
  (:import (clojure.lang Seqable, Atom)))

; choice next
; swap branch multiple threads
; swap as transaction retry all validation for choice
; save monitor
;valid interaction
;   false = exception
;   true = apply, allen succees when de hudige monitor is nog steeds hetzelfde
; keeptrack of send
;Close in DSL

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

;load helper namespace files!
(load "macros"
      "interactions"
      "channels"
      "monitoring"
      "buffers"
      "wildcard"
      "nestedMonitorLinking")

(defn close-infrastructure!
  "Close all channels of the Discourje infrastructure"
  [infra]
  (doseq [c infra] (close! c)))

(defn make-interaction [action sender receiver]
  "Creates an interaction object specifying sending action from sender to receiver."
  (->interaction (uuid/v1) action sender receiver #{} nil))

(defn make-choice
  "Create a choice interaction"
  [branches]
  (->branch (uuid/v1) branches nil))

(defn make-recursion
  "Generate recursion"
  [name recursion]
  (->recursion (uuid/v1) name recursion nil))

(defn do-recur
  "do recur to start of recursion"
  [name]
  (->recur-identifier (uuid/v1) name :recur nil))

(defn make-parallel
  "Generate parallel construct
  Future features to add:
  - `break': When reached on a parallel branch, will end ALL parallels and continue on main path
  - `await': WHen reached on a parallel branch, will wait for other parallels to have reached a certain point (maybe specified by a certain interaction)
  - `':
  "
  [parallels]
  (->parallel (uuid/v1) parallels nil))

(defn create-protocol
  "Generate protocol based on interactions"
  [interactions]
  (->protocol interactions))

(defn generate-monitor
  "Generate the monitor based on the given protocol"
  [protocol]
  (let [linked-interactions (nest-mep (get-interactions protocol))]
    (->monitor (uuid/v1) linked-interactions (atom linked-interactions) (atom {}))))

(defn- all-channels-implement-transportable?
  "Do all custom supplied channels implement the transportable interface?"
  [channels]
  (= 1 (count (distinct (for [c channels] (satisfies? transportable c))))))

(defn generate-infrastructure
  "Generate channels with monitor based on the protocol, also supports arity to give manually created custom channels. With for example: specific buffer requirements."
  ([protocol]
   (let [monitor (generate-monitor protocol)
         roles (get-distinct-role-pairs (get-interactions protocol))
         channels (generate-minimum-channels roles monitor 1)]
     channels))
  ([protocol channels]
   (if (all-channels-implement-transportable? channels)
     (let [roles (get-distinct-role-pairs (get-interactions protocol))
           control-channels (generate-minimum-channels roles monitor 1)
           all-channels-given? (nil? (some #(false? %) (for [channel control-channels] (not (empty? (filter (fn [c] (and (= (:provider c) (:provider channel)) (= (:consumers c) (:consumers channel)))) channels))))))]
       (if all-channels-given?
         (let [monitor (generate-monitor protocol)]
           (vec (for [c channels] (assoc c :monitor monitor))))
         (log-error :invalid-channels "Cannot generate infrastructure, make sure all channels required for the protocol are given!")))
     (log-error :invalid-channels "Cannot generate infrastructure, make sure all supplied channels implement the `transportable' protocol!"))))

(defn- allow-send
  "Allow send message in channel"
  [channel message]
  (async/>!! (get-chan channel) message)
  channel)

(defn- allow-receive
  "Allow a receive on the channel"
  [channel]
  (log-message "allowing receive on channel!")
  (async/<!! (get-chan channel))
  channel)

(defn- allow-sends
  "Allow sending message on multiple channels"
  [channels message]
  (doseq [c channels] (allow-send c message))
  channels)

(defn all-valid-channels?
  "Do all channels comply with the monitor"
  [channels message]
  (= 1 (count (distinct (for [c channels] (valid-send? (get-monitor c) (get-provider c) (get-consumer c) (get-label message)))))))

(defn- can-put?
  "check if the buffer in full, when full wait until there is space in the buffer"
  [channel]
  (or
    (and
      (true? (vector? channel))
      (some #(buffer-full? (get-chan %)) channel))
    (and
      (false? (vector? channel))
      (true? (buffer-full? (get-chan channel))))))

(defn >!!
  "Put on channel"
  [channel message]
  (do (loop []
        (when (can-put? channel) (recur)))
      (let [m (if (satisfies? sendable message)
                message
                (->message (type message) message))]
        (if (vector? channel)
          (cond
            (false? (equal-senders? channel))
            (log-error :invalid-parallel-channels "Trying to send in parallel, but the sender of the channels is not the same!")
            (false? (equal-monitors? channel))
            (log-error :monitor-mismatch "Trying to send in parallel, but the channels do not share the same monitor!")
            (false? (all-valid-channels? channel m))
            (log-error :incorrect-communication "Trying to send in parallel, but the monitor is not correct for all channels!")
            :else
            (allow-sends channel m))
          (if-not (valid-send? (get-monitor channel) (get-provider channel) (get-consumer channel) (get-label m))
            (log-error :incorrect-communication (format "Atomic-send communication invalid! sender: %s, receiver: %s, label: %s while active interaction is: %s" (get-provider channel) (get-consumer channel) (get-label m) (to-string (get-active-interaction (get-monitor channel)))))
            (allow-send channel m))))))

(defn <!!
  "take form channel"
  ([channel]
   (<!! channel nil))
  ([channel label]
   (if (and (nil? label) (false? (get-wildcard)))
     (log-error :wildcard-exception "Taking from a channel without specifying a label is not allowed while wildcards are disabled!")
     (do (loop []
           (when (false? (something-in-buffer? (get-chan channel))) (recur)))
         (if (nil? (get-active-interaction (get-monitor channel)))
           (log-error :invalid-monitor "Please activate a monitor, your protocol has not yet started, or it is already finished!")
           (let [result (peek-channel (get-chan channel))
                 monitor-and-label-check (and (or (and (nil? label) (true? (get-wildcard)))
                                                  (= (get-label result) label)
                                                  (contains-value? (get-label result) label))
                                              (valid-receive? (get-monitor channel) (get-provider channel) (get-consumer channel) label))]
             (when-not monitor-and-label-check
               (log-error :incorrect-communication (format "Atomic-receive communication invalid! sender: %s, receiver: %s, label: %s while active interaction is: %s" (get-provider channel) (get-consumer channel) label (to-string (get-active-interaction (get-monitor channel))))))
             (if (or (and (nil? label) (true? (get-wildcard))) (= (get-label result) label) (contains-value? (get-label result) label))
               (do (apply-receive! (get-monitor channel) (get-provider channel) (get-consumer channel) label)
                   (allow-receive channel)
                   result)
               (log-error :incorrect-communication (format "communication invalid! message label on channel: %s does not match the target label %s!" (get-label result) label)))))))))

(defn <!!!
  "take form channel peeking, and delay receive when parallel"
  ([channel]
   (<!!! channel nil))
  ([channel label]
   (if (and (nil? label) (false? (get-wildcard)))
     (log-error :wildcard-exception "Taking from a channel without specifying a label is not allowed while wildcards are disabled!")
     (do (loop []
           (when (false? (something-in-buffer? (get-chan channel))) (recur)))
         (if (nil? (get-active-interaction (get-monitor channel)))
           (log-error :invalid-monitor "Please activate a monitor, your protocol has not yet started, or it is already finished!")
           (let [result (peek-channel (get-chan channel))
                 isParallel (is-current-multicast? (get-monitor channel) label)
                 id (get-id (get-active-interaction (get-monitor channel)))
                 monitor-and-label-check (and (or (and (nil? label) (true? (get-wildcard)))
                                                  (= (get-label result) label)
                                                  (contains-value? (get-label result) label))
                                              (valid-receive? (get-monitor channel) (get-provider channel) (get-consumer channel) label))]
             (when-not monitor-and-label-check
               (log-error :incorrect-communication (format "Atomic-receive communication invalid! sender: %s, receiver: %s, label: %s while active interaction is: %s" (get-provider channel) (get-consumer channel) label (to-string (get-active-interaction (get-monitor channel))))))
             (if (or (and (nil? label) (true? (get-wildcard))) (= (get-label result) label) (contains-value? (get-label result) label))
               (do (apply-receive! (get-monitor channel) (get-provider channel) (get-consumer channel) label)
                   (allow-receive channel)
                   (loop [par isParallel]
                     (when (true? par) (recur (= id (get-id (get-active-interaction (get-monitor channel)))))))
                   result)
               (log-error :incorrect-communication (format "communication invalid! message label on channel: %s does not match the target label %s!" (get-label result) label)))))))))