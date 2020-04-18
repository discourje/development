(ns discourje.core.async
  (:require [clojure.core.async :as a]
            [discourje.spec :as s]
            [discourje.spec.lts :as lts]
            [discourje.core.async.impl.buffers :as buffers]
            [discourje.core.async.impl.channels :as channels]
            [discourje.core.async.impl.monitors :as monitors]))

(defn monitor [spec]
  (monitors/monitor (lts/lts spec false)))

;;;;
;;;; CORE CONCEPTS: chan, close!
;;;;

(defn chan
  ([]
   (chan nil nil nil nil))
  ([buf-or-n]
   (chan buf-or-n nil nil nil nil))
  (;[buf-or-n xform]
   [_ _]
   (throw (UnsupportedOperationException.)))
  (;[buf-or-n xform ex-handler]
   [_ _ _]
   (throw (UnsupportedOperationException.)))
  ([sender receiver monitor config]
   (channels/unbuffered-channel sender
                                receiver
                                monitor))
  ([buf-or-n sender receiver monitor config]
   (channels/buffered-channel (if (number? buf-or-n)
                                (buffers/fixed-buffer buf-or-n)
                                buf-or-n)
                              sender
                              receiver
                              monitor))
  (;[buf-or-n xform sender receiver monitor config]
   [_ _ _ _ _ _]
   (throw (UnsupportedOperationException.)))
  (;[buf-or-n xform ex-handler sender receiver monitor config]
   [_ _ _ _ _ _ _]
   (throw (UnsupportedOperationException.))))

(defn close!
  [chan]
  (channels/close! chan))

;;;;
;;;; CORE CONCEPTS: >!!, <!!, thread
;;;;

(defn >!!
  [port val]
  (channels/>!! port val))

(defn <!!
  [port]
  (channels/<!! port))

(defmacro thread
  [& body]
  `(let [m# (monitors/monitor (s/* (s/any #{"sender" "receiver"})))
         c# (channels/buffered-channel 1 (s/role "sender") (s/role "receiver") m#)]
     (a/take! (a/thread-call (^:once fn* [] ~@body))
              (fn [x#]
                (if (not (nil? x#))
                  (channels/>!! c# x#))
                (channels/close! c#)))
     c#))

;;;;
;;;; CORE CONCEPTS: >!, <!, go
;;;;

;; TODO

;;;;
;;;; CORE CONCEPTS: alts!, alts!!, timeout
;;;;

;; TODO

;;;;
;;;; CORE CONCEPTS: dropping-buffer, sliding-buffer
;;;;

(defn dropping-buffer
  [n]
  (buffers/dropping-buffer n))

(defn sliding-buffer
  [n]
  (buffers/sliding-buffer n))

;;;;
;;;; MORE CONCEPTS
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

;(defn unblocking-buffer?
;  [buff]
;  (buffers/unblocking-buffer? buff))