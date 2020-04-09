(ns discourje.core.async
  (:require [discourje.core.async.impl.channels :as channels]))

;;;;
;;;; role and chan
;;;;

(defn chan
  ([sender receiver monitor]
   (chan 0 sender receiver monitor))
  ([buf-or-n sender receiver monitor]
   (channels/channel buf-or-n sender receiver monitor))
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