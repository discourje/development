(ns discourje.core.async
  (:require [clj-uuid :as uuid]
            [discourje.core.logging :refer :all]
            [clojure.core.async :as async]
            [clojure.core.async.impl.protocols :as bufs])
  (:import (clojure.lang Seqable)))

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
      "interactionLinking"
      "buffers")

(defn make-interaction [action sender receiver]
  "Creates an interaction object specifying sending action from sender to receiver."
  (->interaction (uuid/v1) action sender receiver nil))

(defn make-choice
  "Create a choice interaction"
  [branches]
  (->branch (uuid/v1) branches nil))

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

(defn create-protocol
  "Generate protocol based on interactions"
  [interactions]
  (->protocol interactions))

(defn generate-monitor
  "Generate the monitor based on the given protocol"
  [protocol]
  (let [linked-interactions (link-interactions protocol)]
    (->monitor (uuid/v1) linked-interactions (atom (first linked-interactions)))))

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
  (async/>!! (get-chan channel) message))

(defn- allow-receive
  "Allow a receive on the channel"
  [channel]
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

(defn >!!
  "Put on channel"
  [channel message]
  (let [m (if (satisfies? sendable message)
            message
            (->message (type message) message))]
    (if (vector? channel)
      (do (when-not (equal-senders? channel)
            (log-error :invalid-parallel-channels "Trying to send in parallel, but the sender of the channels is not the same!"))
          (when-not (equal-monitors? channel)
            (log-error :monitor-mismatch "Trying to send in parallel, but the channels do not share the same monitor!"))
          (when-not (all-valid-channels? channel m)
            (log-error :incorrect-communication "Trying to send in parallel, but the monitor is not correct for all channels!"))
          (allow-sends channel m))
      (do (when-not (valid-interaction? (get-monitor channel) (get-provider channel) (get-consumer channel) (get-label m))
            (log-error :incorrect-communication (format "Atomic-send communication invalid! sender: %s, receiver: %s, label: %s while active interaction is: %s" (get-provider channel) (get-consumer channel) (get-label m) (to-string (get-active-interaction (get-monitor channel))))))
          (allow-send channel m)))))

(defn >!!!
  "Poll buffer to be full, when there is a free spot, try put"
  [channel message]
  (loop []
    (when (or
            (and
              (true? (vector? channel))
              (some #(buffer-full? (get-chan %)) channel))
            (and
              (false? (vector? channel))
              (true? (buffer-full? (get-chan channel))))) (recur)))
  (>!! channel message))

(defn <!!
  "Take from channel"
  ([channel]
   (<!! channel nil))
  ([channel label]
   (if (nil? (get-active-interaction (get-monitor channel)))
     (log-error :invalid-monitor "Please activate a monitor, your protocol has not yet started, or it is already finished!")
     (let [result (allow-receive channel)]
       (do (when-not (or (valid-interaction? (get-monitor channel) (get-provider channel) (get-consumer channel) label) (or (nil? label) (= (get-label result) label) (contains-value? (get-label result) label)))
             (log-error :incorrect-communication (format "Atomic-send communication invalid! sender: %s, receiver: %s, label: %s while active interaction is: %s" (get-provider channel) (get-consumer channel) label (to-string (get-active-interaction (get-monitor channel))))))
           (apply-interaction (get-monitor channel) (get-provider channel) (get-consumer channel) label)
           result)))))

(defn <!!!
  "take form channel peeking"
  ([channel]
   (<!!! channel nil))
  ([channel label]
   (loop []
     (when (false? (something-in-buffer? (get-chan channel))) (recur)))
   (if (nil? (get-active-interaction (get-monitor channel)))
     (log-error :invalid-monitor "Please activate a monitor, your protocol has not yet started, or it is already finished!")
     (let [result (peek-channel (get-chan channel))]
       (do (when-not (or (valid-interaction? (get-monitor channel) (get-provider channel) (get-consumer channel) label) (or (nil? label) (= (get-label result) label) (contains-value? (get-label result) label)))
             (log-error :incorrect-communication (format "Atomic-send communication invalid! sender: %s, receiver: %s, label: %s while active interaction is: %s" (get-provider channel) (get-consumer channel) label (to-string (get-active-interaction (get-monitor channel))))))
           (apply-interaction (get-monitor channel) (get-provider channel) (get-consumer channel) label)
           (allow-receive channel)
           result)))))

(defn <!!!!
  "take form channel peeking, and delay receive on parallel"
  ([channel]
   (<!!! channel nil))
  ([channel label]
   (loop []
     (when (false? (something-in-buffer? (get-chan channel))) (recur)))
   (if (nil? (get-active-interaction (get-monitor channel)))
     (log-error :invalid-monitor "Please activate a monitor, your protocol has not yet started, or it is already finished!")
     (let [result (peek-channel (get-chan channel))
           isParallel (is-current-parallel? (get-monitor channel) label)
           id (get-id (get-active-interaction (get-monitor channel)))]
       (do (when-not (or (valid-interaction? (get-monitor channel) (get-provider channel) (get-consumer channel) label) (or (nil? label) (= (get-label result) label) (contains-value? (get-label result) label)))
             (log-error :incorrect-communication (format "Atomic-send communication invalid! sender: %s, receiver: %s, label: %s while active interaction is: %s" (get-provider channel) (get-consumer channel) label (to-string (get-active-interaction (get-monitor channel))))))
           (apply-interaction (get-monitor channel) (get-provider channel) (get-consumer channel) label)
           (allow-receive channel)
           (loop [par isParallel]
             (when (true? par)(recur (= id (get-id (get-active-interaction (get-monitor channel)))))))
           result)))))