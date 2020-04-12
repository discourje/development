(ns discourje.core.async
  (:require [clojure.core.async :as a]
            [discourje.spec :as s]
            [discourje.core.async.impl.buffers :as buffers]
            [discourje.core.async.impl.channels :as channels]))

;;;;
;;;; chan
;;;;

(defn chan
  ([sender receiver monitor]
   (chan 0 sender receiver monitor))
  ([buf-or-n sender receiver monitor]
   (channels/channel (if (number? buf-or-n)
                       (buffers/fixed-buffer buf-or-n)
                       buf-or-n)
                     sender
                     receiver
                     monitor))
  (;[buf-or-n xform sender receiver monitor]
   [_ _ _ _ _]
   (throw (UnsupportedOperationException.)))
  (;[buf-or-n xform ex-handler sender receiver monitor]
   [_ _ _ _ _ _]
   (throw (UnsupportedOperationException.))))

;;;;
;;;; put! and take!
;;;;

(defn put!
  ([port val]
   (put! port val identity))
  ([port val fn1]
   (channels/put! port val fn1))
  (;[port val fn1 on-caller?]
   [_ _ _ _]
   (throw (UnsupportedOperationException.))))

(defn take!
  ([port fn1]
   (channels/take! port fn1))
  (;[port fn1 on-caller?]
   [_ _ _]
   (throw (UnsupportedOperationException.))))

;;;;
;;;; >!!, <!!, and thread
;;;;

(defn >!!
  [port val]
  (channels/>!! port val))

(defn <!!
  [port]
  (channels/<!! port))

(defmacro thread
  [& body]
  `(let [m# (s/monitor (s/* (s/any #{"sender" "receiver"})))
         c# (channels/channel 1 (s/role "sender") (s/role "receiver") m#)]
     (a/take! (a/thread-call (^:once fn* [] ~@body))
              (fn [x#]
                (if (not (nil? x#))
                  (channels/>!! c# x#))
                (channels/close! c#)))
     c#))

;;;;
;;;; >!, <!, and go
;;;;

;; TODO

;;;;
;;;; dropping-buffer and sliding-buffer
;;;;

(defn dropping-buffer
  [n]
  (buffers/dropping-buffer n))

(defn sliding-buffer
  [n]
  (buffers/sliding-buffer n))

(defn unblocking-buffer?
  [buff]
  (buffers/unblocking-buffer? buff))

;;;;
;;;; close!
;;;;

(defn close!
  [chan]
  (channels/close! chan))

;;;;
;;;; alts! and alts!!
;;;;

;; TODO