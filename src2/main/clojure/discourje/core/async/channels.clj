(ns discourje.core.async.channels
  (:require [clojure.core.async :as a]
            [discourje.core.async.buffers :as buffers]
            [discourje.core.async.monitors :as monitors]
            [discourje.core.spec.ast :as ast]
            [discourje.core.spec.interp :as interp]))

(definterface MutableSender
  (getSender [])
  (setSender [newSender]))

(definterface MutableReceiver
  (getReceiver [])
  (setReceiver [newReceiver]))

(definterface MutableMonitor
  (getMonitor [])
  (setMonitor [newMonitor]))

(deftype Channel
  [buffered
   ch
   ch-ghost1
   ch-ghost2
   ^:volatile-mutable sender
   ^:volatile-mutable receiver
   ^:volatile-mutable monitor]

  MutableSender
  (getSender [_] sender)
  (setSender [_ newSender] (set! sender newSender))

  MutableReceiver
  (getReceiver [_] receiver)
  (setReceiver [_ newReceiver] (set! receiver newReceiver))

  MutableMonitor
  (getMonitor [_] monitor)
  (setMonitor [_ newMonitor] (set! monitor newMonitor)))

(defn channel? [x]
  (= (type x) Channel))

(defn- channel [buffered ch ch-ghost1 ch-ghost2]
  {:pre []}
  (->Channel buffered ch ch-ghost1 ch-ghost2 nil nil nil))

(defn unbuffered-channel []
  {:pre []}
  (channel false (a/chan) (a/chan) nil))

(defn buffered-channel [buffer]
  {:pre [(buffers/buffer? buffer)]}
  (let [chan (fn [buffer]
               (case (buffers/type buffer)
                 :fixed-buffer (a/chan (buffers/n buffer))
                 :dropping-buffer (a/chan (a/dropping-buffer (buffers/n buffer)))
                 :sliding-buffer (a/chan (a/sliding-buffer (buffers/n buffer)))
                 :promise-buffer (throw (IllegalArgumentException.))))]
    (channel true (chan buffer) (chan buffer) (chan buffer))))

(defn link [this r1 r2 m]
  {:pre [(channel? this)
         (or (ast/role? r1) (fn? r1))
         (or (ast/role? r2) (fn? r2))
         (monitors/monitor? m)]}
  (.setSender this (interp/eval-role r1))
  (.setReceiver this (interp/eval-role r2))
  (.setMonitor this m)
  this)

(defonce ^:private token 0)
(defonce ^:private sync-not-ok (Object.))

;;;;
;;;; close!
;;;;

(defn close!
  [channel]
  {:pre [(channel? channel)]}

  (let [result (monitors/verify! (.getMonitor channel)
                                 :close
                                 nil
                                 (.getSender channel)
                                 (.getReceiver channel))]
    (if (.-buffered channel)

      ;; Buffered channel
      (if (true? result)

        ;; Commit
        (do (a/close! (.-ch channel))
            (monitors/lower-flag! (.getMonitor channel))
            (a/close! (.-ch_ghost1 channel))
            (a/close! (.-ch_ghost2 channel)))

        ;; Abort
        (throw result))

      ;; Unbuffered channel
      (if (true? result)

        ;; Commit
        (do (a/close! (.-ch channel))
            (monitors/lower-flag! (.getMonitor channel))
            (a/close! (.-ch_ghost1 channel)))

        ;; Abort
        (throw result)))))

;;;;
;;;; >!! and <!!
;;;;

(defn- >!!-step1
  [channel]
  {:pre [(channel? channel)]}
  (a/>!! (.-ch_ghost1 channel) token))

(defn- >!!-step2
  [channel message]
  {:pre [(channel? channel)]}
  (if (.-buffered channel)

    ;; Buffered channel
    (let [result (monitors/verify! (.getMonitor channel)
                                   :send
                                   message
                                   (.getSender channel)
                                   (.getReceiver channel))]
      (if (true? result)

        ;; Commit
        (let [x (a/>!! (.-ch channel) message)
              _ (monitors/lower-flag! (.getMonitor channel))
              _ (a/>!! (.-ch_ghost2 channel) token)]
          x)

        ;; Abort
        (do (a/<!! (.-ch_ghost1 channel))
            (throw result))))

    ;; Unbuffered channel
    (let [result (monitors/verify! (.getMonitor channel)
                                   :sync
                                   message
                                   (.getSender channel)
                                   (.getReceiver channel))]
      (if (true? result)

        ;; Commit
        (let [x (a/>!! (.-ch channel) message)
              _ (monitors/lower-flag! (.getMonitor channel))]
          x)

        ;; Abort
        (do (a/>!! (.-ch channel) sync-not-ok)
            (a/>!! (.-ch channel) result)
            (throw result))))))

(defn >!!
  [channel message]
  {:pre [(channel? channel)]}
  (>!!-step1 channel)
  (>!!-step2 channel message))

(defn <!!-step1
  [channel]
  {:pre [(channel? channel)]}
  (if (.-buffered channel)
    (a/<!! (.-ch_ghost2 channel))
    (a/<!! (.-ch_ghost1 channel))))

(defn <!!-step2
  [channel]
  {:pre [(channel? channel)]}
  (if (.-buffered channel)

    ;; Buffered channel
    (let [result (monitors/verify! (.getMonitor channel)
                                   :receive
                                   nil
                                   (.getSender channel)
                                   (.getReceiver channel))]
      (if (true? result)

        ;; Commit
        (let [message (a/<!! (.ch channel))
              _ (monitors/lower-flag! (.getMonitor channel))
              _ (a/<!! (.-ch_ghost1 channel))]
          message)

        ;; Abort
        (do (a/>!! (.-ch_ghost2 channel) token)
            (throw result))))

    ;; Unbuffered channel
    (let [message (a/<!! (.-ch channel))]
      (if (= message sync-not-ok)
        (throw (a/<!! (.-ch channel)))
        message))))

(defn <!!
  [channel]
  {:pre [(channel? channel)]}
  (<!!-step1 channel)
  (<!!-step2 channel))

;;;;
;;;; >! and <!
;;;;

;; TODO

;;;;
;;;; alts! and alts!!
;;;;

;; TODO: alts!

(defn alts!!
  [alternatives opts]
  {:pre [(every? #(or (and (vector? %) (= 2 (count %)) (channel? (first %)))
                      (channel? %))
                 alternatives)]}

  (let [ports (mapv #(if (vector? %)
                       (let [channel (first %)
                             port (.-ch_ghost1 channel)]
                         [port token])
                       (let [channel %
                             port (if (.-buffered channel)
                                    (.-ch_ghost2 channel)
                                    (.-ch_ghost1 channel))]
                         port))
                    alternatives)

        [val port] (if (nil? opts)
                     (a/alts!! ports)
                     (if (contains? opts :default)
                       (if (contains? opts :priority)
                         (a/alts!! ports :default (:default opts) :priority (:priority opts))
                         (a/alts!! ports :default (:default opts)))
                       (if (contains? opts :priority)
                         (a/alts!! ports :priority (:priority opts))
                         (a/alts!! ports))))

        alternative (if (= port :default)
                      nil
                      (loop [todo alternatives]
                        (if (empty? todo)
                          (throw (Exception.))
                          (let [alternative (first todo)]
                            (if (vector? alternative)
                              (let [channel (first alternative)]
                                (if (= port (.-ch_ghost1 channel))
                                  alternative
                                  (recur (rest todo))))
                              (let [channel alternative]
                                (if (= port (if (.-buffered channel)
                                              (.-ch_ghost2 channel)
                                              (.-ch_ghost1 channel)))
                                  alternative
                                  (recur (rest todo)))))))))]

    (if (nil? alternative)
      [val :default]
      (if (vector? alternative)
        (let [channel (first alternative)]
          [(>!!-step2 channel (second alternative)) channel])
        (let [channel alternative]
          [(<!!-step2 channel) channel])))))

;;;;
;;;; put! and take!
;;;;

;;; *** Untested code: ***
;
;;; To do: Add lower-flag! after positive verify!
;
;(defn put!
;  [channel message f]
;  {:pre [(channel? channel) (fn? f)]}
;  (if (.-buffered channel)
;
;    ;; Buffered channel
;    (a/put! (.-ch_ghost1 channel)
;            token
;            (fn [_] (if (monitors/verify! (.getMonitor channel)
;                                          :send
;                                          message
;                                          (.getSender channel)
;                                          (.getReceiver channel))
;
;                      ;; If ok, commit
;                      (a/put! (.-ch channel)
;                              message
;                              (fn [x] (a/put! (.-ch_ghost2 channel)
;                                              token
;                                              (fn [_] (f x)))))
;
;                      ;; If not ok, abort
;                      (a/take! (.-ch_ghost1 channel)
;                               (fn [_] (throw (RuntimeException.)))))))
;
;    ;; Unbuffered channel
;    (a/put! (.-ch_ghost1 channel)
;            token
;            (fn [_] (if (monitors/verify! (.getMonitor channel)
;                                          :sync
;                                          message
;                                          (.getSender channel)
;                                          (.getReceiver channel))
;
;                      (a/put! (.-ch channel)
;                              message
;                              f)
;
;                      (a/put! (.-ch channel)
;                              sync-not-ok
;                              (fn [_] (throw (RuntimeException.)))))))))
;
;(defn take!
;  [channel f]
;  {:pre [(channel? channel) (fn? f)]}
;  (if (.-buffered channel)
;
;    ;; Buffered channel
;    (a/take! (.-ch_ghost2 channel)
;             (fn [_] (if (monitors/verify! (.getMonitor channel)
;                                           :receive
;                                           nil
;                                           (.getSender channel)
;                                           (.getReceiver channel))
;
;                       ;; If ok, commit
;                       (a/take! (.-ch channel)
;                                (fn [x] (a/take! (.-ch_ghost1 channel)
;                                                 (fn [_] (f x)))))
;
;                       ;; If not ok, abort
;                       (a/put! (.-ch_ghost2 channel)
;                               token
;                               (fn [_] (throw (RuntimeException.)))))))
;
;    ;; Unbuffered channel
;    (a/take! (.-ch_ghost1 channel)
;             (fn [_] (a/take! (.-ch channel)
;                              (fn [x] (if (= x sync-not-ok)
;                                        (throw (RuntimeException.))
;                                        (f x))))))))