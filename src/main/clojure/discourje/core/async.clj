(ns discourje.core.async
  (:gen-class)
  (:require [clojure.core.async :as a]
            [discourje.core.async.buffers :as buffers]
            [discourje.core.async.channels :as channels]
            [discourje.core.async.monitors :as monitors]
            [discourje.core.spec.lts :as lts]))

(defn monitor [spec & {:keys [on-the-fly history]
                       :or   {on-the-fly true, history false}}]
  (monitors/monitor (lts/lts spec :on-the-fly on-the-fly :history history)))

(defn link
  ([channel sender receiver monitor]
   (link channel sender receiver monitor nil))
  ([channel sender receiver monitor options]
   (channels/link channel sender receiver monitor)))

;;;;
;;;; CORE CONCEPTS: chan, close!
;;;;

(defn chan
  ([]
   (channels/unbuffered-channel))
  ([buf-or-n]
   (channels/buffered-channel (if (number? buf-or-n)
                                (buffers/fixed-buffer buf-or-n)
                                buf-or-n)))
  (;[buf-or-n xform]
   [_ _]
   (throw (UnsupportedOperationException.)))
  (;[buf-or-n xform ex-handler]
   [_ _ _]
   (throw (UnsupportedOperationException.)))
  ([sender receiver monitor options]
   (link (chan) sender receiver monitor options))
  ([buf-or-n sender receiver monitor options]
   (link (chan buf-or-n) sender receiver monitor options))
  (;[buf-or-n xform sender receiver monitor options]
   [_ _ _ _ _ _]
   (throw (UnsupportedOperationException.)))
  (;[buf-or-n xform ex-handler sender receiver monitor options]
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
  (let [clj (macroexpand `(a/thread ~@body))]
    `(let [c# (chan 1)]
       (a/take! ~clj
                (fn [x#]
                  (if (not (nil? x#))
                    (channels/>!! c# x#))
                  (channels/close! c#)))
       c#)))

;;;;
;;;; CORE CONCEPTS: >!, <!, go
;;;;

;; TODO

;;;;
;;;; CORE CONCEPTS: alts!!, alts!, timeout
;;;;

(defn alts!!
  [ports & {:as opts}]
  (channels/alts!! ports opts))

;; TODO: alts!

(defn timeout
  [msecs]
  (let [c (chan 1)]
    (a/take! (a/timeout msecs) (fn [_] (close! c)))
    c))

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
;;;; OTHER
;;;;

;;; *** Untested code: ***
;
;(defn put!
;  ([port val]
;   (put! port val identity))
;  ([port val fn1]
;   (channels/put! port val fn1))
;  (;[port val fn1 on-caller?]
;   [_ _ _ _]
;   (throw (UnsupportedOperationException.))))
;
;(defn take!
;  ([port fn1]
;   (channels/take! port fn1))
;  (;[port fn1 on-caller?]
;   [_ _ _]
;   (throw (UnsupportedOperationException.))))
;
;(defn unblocking-buffer?
;  [buff]
;  (buffers/unblocking-buffer? buff))