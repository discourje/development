(ns discourje.core.async.impl.channels
  (:require [clojure.core.async :as a]
            [discourje.spec.ast :as ast]
            [discourje.spec.interp :as interp]
            [discourje.core.async.impl.buffers :as buffers]
            [discourje.core.async.impl.monitors :as monitors]))

(deftype Channel [buffered ch ch-ghost1 ch-ghost2 sender receiver monitor])

(defn channel? [x]
  {:pre [true]}
  (= (type x) Channel))

(defn channel [buffer sender receiver monitor]
  {:pre [(buffers/buffer? buffer)
         (ast/role? sender)
         (ast/role? receiver)
         (monitors/monitor? monitor)]}
  (->Channel (> (buffers/capacity buffer) 0)
             (buffers/clojure-core-async-chan buffer)
             (buffers/clojure-core-async-chan buffer)
             (buffers/clojure-core-async-chan buffer)
             (interp/eval-role sender)
             (interp/eval-role receiver)
             monitor))

(defonce token 0)
(defonce sync-not-ok (Object.))

(defn verify-sync! [message channel]
  (monitors/verify-sync! message
                         (.-sender channel)
                         (.-receiver channel)
                         (.-monitor channel)))

(defn verify-send! [message channel]
  (monitors/verify-send! message
                         (.-sender channel)
                         (.-receiver channel)
                         (.-monitor channel)))

(defn verify-receive! [channel]
  (monitors/verify-receive! (.-sender channel)
                            (.-receiver channel)
                            (.-monitor channel)))

(defn verify-close! [channel]
  (monitors/verify-close! (.-sender channel)
                          (.-receiver channel)
                          (.-monitor channel)))

;;;;
;;;; put! and take!
;;;;

(defn put!
  [channel message f]
  {:pre [(channel? channel) (fn? f)]}
  (if (.-buffered channel)

    ;; Buffered channel
    (a/put! (.-ch_ghost1 channel)
            token
            (fn [_] (if (verify-send! message channel)

                      ;; If ok, commit
                      (a/put! (.-ch channel)
                              message
                              (fn [x] (a/put! (.-ch_ghost2 channel)
                                              token
                                              (fn [_] (f x)))))

                      ;; If not ok, abort
                      (a/take! (.-ch_ghost1 channel)
                               (fn [_] (throw (RuntimeException.)))))))

    ;; Unbuffered channel
    (a/put! (.-ch_ghost1 channel)
            token
            (fn [_] (if (verify-sync! message channel)
                      (a/put! (.-ch channel)
                              message
                              f)
                      (a/put! (.-ch channel)
                              sync-not-ok
                              (fn [_] (throw (RuntimeException.)))))))))

(defn take!
  [channel f]
  {:pre [(channel? channel) (fn? f)]}
  (if (.-buffered channel)

    ;; Buffered channel
    (a/take! (.-ch_ghost2 channel)
             (fn [_] (if (verify-receive! channel)

                       ;; If ok, commit
                       (a/take! (.-ch channel)
                                (fn [x] (a/take! (.-ch_ghost1 channel)
                                                 (fn [_] (f x)))))

                       ;; If not ok, abort
                       (a/put! (.-ch_ghost2 channel)
                               token
                               (fn [_] (throw (RuntimeException.)))))))

    ;; Unbuffered channel
    (a/take! (.-ch_ghost1 channel)
             (fn [_] (a/take! (.-ch channel)
                              (fn [x] (if (= x sync-not-ok)
                                        (throw (RuntimeException.))
                                        (f x))))))))

;;;;
;;;; >!! and <!!
;;;;

(defn >!!
  [channel message]
  {:pre [(channel? channel)]}
  (if (.-buffered channel)

    ;; Buffered channel
    (do (a/>!! (.-ch_ghost1 channel) token)
        (if (verify-send! message channel)

          ;; If ok, commit
          (let [x (a/>!! (.-ch channel) message)
                _ (a/>!! (.-ch_ghost2 channel) token)]
            x)

          ;; If not ok, abort
          (do (a/<!! (.-ch_ghost1 channel))
              (throw (RuntimeException.)))))

    ;; Unbuffered channel
    (do (a/>!! (.-ch_ghost1 channel) token)
        (if (verify-sync! message channel)
          (do (a/>!! (.-ch channel) message))
          (do (a/>!! (.-ch channel) sync-not-ok)
              (throw (RuntimeException.)))))))

(defn <!!
  [channel]
  {:pre [(channel? channel)]}
  (if (.-buffered channel)

    ;; Buffered channel
    (do (a/<!! (.-ch_ghost2 channel))
        (if (verify-receive! channel)

          ;; If ok, commit
          (let [message (a/<!! (.ch channel))
                _ (a/<!! (.-ch_ghost1 channel))]
            message)

          ;; If not ok, abort
          (do (a/>!! (.-ch_ghost2 channel) token)
              (throw (RuntimeException.)))))

    ;; Unbuffered channel
    (do (a/<!! (.-ch_ghost1 channel))
        (let [message (a/<!! (.-ch channel))]
          (if (= message sync-not-ok)
            (throw (RuntimeException.))
            message)))))

;;;;
;;;; close!
;;;;

(defn close!
  [channel]
  {:pre [(channel? channel)]}
  (if (.-buffered channel)

    ;; Buffered channel
    (if (verify-close! channel)
      (do (a/close! (.-ch_ghost1 channel))
          (a/close! (.-ch channel))
          (a/close! (.-ch_ghost2 channel))
          )
      (fn [_] (throw (RuntimeException.))))

    ;; Unbuffered channel
    (if (verify-close! channel)
      (do (a/close! (.-ch_ghost1 channel))
          (a/close! (.-ch channel)))
      (fn [_] (throw (RuntimeException.))))))