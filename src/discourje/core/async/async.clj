(ns discourje.core.async.async
  (:require [clojure.core.async :rename {>!! core->!! <!! core-<!!}]
            [clj-uuid :as uuid])
  (:import (clojure.lang Seqable))
  )

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

(defn create-protocol [interactions]
  "Generate protocol based on interactions"
  (->protocol interactions))

(defn- link-interactions [protocol]
  (let [interactions (get-interactions protocol)
        helper-vec (atom [])
        linked-interactions (atom [])]
    (if (= 1 (count interactions))
      interactions
      (do (doseq [inter interactions]
            (cond
              (empty? @helper-vec) (swap! helper-vec conj inter)
              (instance? interaction inter) (let [i (last @helper-vec)
                                                  linked-i (assoc i :next (get-id inter))]
                                              (swap! helper-vec conj inter)
                                              (swap! linked-interactions conj linked-i))))
          (swap! linked-interactions conj (last @helper-vec))))
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




(defn- allow-send [channel message active-interaction]
  (cond
    (instance? interaction active-interaction)
    (println "busy")                                        ;todo continue here!
    :else (println "Allowing send on NOT-implemented type of interaction!")
    ))

(defn- allow-sends [channels message active-interaction]
  (doseq [c channels] (allow-send c message active-interaction)))

(defn all-valid-channels?
  "Do all channels comply with the monitor"
  [channels message]
  (= 1 (count (distinct (for[c channels] (valid-interaction? (get-monitor c) (get-provider c) (get-consumer c) (get-label message)))))))

(defn >!!
  "Put on channel"
  [channel message]
  (if (nil? (get-active-interaction (get-monitor channel)))
    (println "Please activate a monitor, your protocol has not yet started, or it is already finished!")
    (if (instance? Seqable channel)
      (do (when-not (equal-senders? channel)
            (println "Trying to send in parallel, but the sender of the channels is not the same!"))
          (when-not (equal-monitors? channel)
            (println "Trying to send in parallel, but the channels do not share the same monitor!"))
          (when-not (all-valid-channels? channel message)
            (println "Trying to send in parallel, but the monitor is not correct for all of them!"))
          (let [monitor (get-monitor (first channel))
                active-interaction (get-active-interaction monitor)]
            (apply-interaction monitor (get-label message))
            (allow-sends channel message active-interaction)))
      (do (when-not (valid-interaction? (get-monitor channel) (get-provider channel) (get-consumer channel) (get-label message))
            (println "Communication invalid!"))
          (let [active-interaction (get-active-interaction (get-monitor channel))]
            (apply-interaction (get-monitor channel) (get-label message))
            (allow-send channel message active-interaction))))))

(defn <!!
  "Take from channel"
  [message channel])
;(defn get-transitions-in-protocol [protocol]
;  (interactions-to-transitions (get-interactions protocol)))
;
;(defn- generate-io-fsms
;  "Convert a protocol of interactions to IO enabled finite-state-machines local to each role."
;  [protocol]
;  (let [roles (get-distinct-roles (get-interactions protocol))]))

