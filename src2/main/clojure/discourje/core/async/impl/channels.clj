(ns discourje.core.async.impl.channels
  (:require [clojure.core.async :as a]
            [discourje.spec.ast :as ast]
            [discourje.spec.interp :as interp]
            [discourje.core.async.impl.buffers :as buffers]
            [discourje.core.async.impl.monitors :as monitors]))

(deftype Channel [buffered ch ch-ghost1 ch-ghost2 sender receiver monitor])

(defn channel? [x]
  (= (type x) Channel))

(defn- clojure-core-async-chan
  ([]
   (a/chan))
  ([buffer]
   {:pre [(buffers/buffer? buffer)]}
   (case (buffers/type buffer)
     :fixed-buffer (a/chan (buffers/n buffer))
     :dropping-buffer (a/chan (a/dropping-buffer (buffers/n buffer)))
     :sliding-buffer (a/chan (a/sliding-buffer (buffers/n buffer)))
     :promise-buffer (throw (IllegalArgumentException.)))))

(defn unbuffered-channel [sender receiver monitor]
  {:pre [(or (ast/role? sender) (nil? sender))
         (or (ast/role? receiver) (nil? receiver))
         (or (monitors/monitor? monitor) (nil? monitor))]}
  (->Channel false
             (clojure-core-async-chan)
             (clojure-core-async-chan)
             nil
             (if (nil? sender) nil (interp/eval-role sender))
             (if (nil? receiver) nil (interp/eval-role receiver))
             monitor))

(defn buffered-channel [buffer sender receiver monitor]
  {:pre [(buffers/buffer? buffer)
         (or (ast/role? sender) (nil? sender))
         (or (ast/role? receiver) (nil? receiver))
         (or (monitors/monitor? monitor) (nil? monitor))]}
  (->Channel true
             (clojure-core-async-chan buffer)
             (clojure-core-async-chan buffer)
             (clojure-core-async-chan buffer)
             (if (nil? sender) nil (interp/eval-role sender))
             (if (nil? receiver) nil (interp/eval-role receiver))
             monitor))

(defonce token 0)
(defonce sync-not-ok (Object.))

(defn- runtime-exception [type message channel]
  (ex-info (str "Action "
                (case type :sync "‽" :send "!" :receive "?" :close "C" (throw (Exception.)))
                "("
                (if (contains? #{:sync :send} type) (str message ",") "")
                (.-sender channel)
                ","
                (.-receiver channel)
                ") is not enabled in current state(s): "
                (monitors/str-current-states (.-monitor channel))
                ". LTS in Aldebaran format (http://cadp.inria.fr/man/aut.html):\n\n"
                (monitors/str-lts (.-monitor channel)))
           {:message message
            :channel channel}))

;;;;
;;;; close!
;;;;

(defn close!
  [channel]
  {:pre [(channel? channel)]}
  (if (.-buffered channel)

    ;; Buffered channel
    (if (monitors/verify! (.-monitor channel)
                          :close
                          nil
                          (.-sender channel)
                          (.-receiver channel))

      (do (a/close! (.-ch_ghost1 channel))
          (a/close! (.-ch channel))
          (a/close! (.-ch_ghost2 channel)))

      (throw (runtime-exception :close nil channel)))

    ;; Unbuffered channel
    (if (monitors/verify! (.-monitor channel)
                          :close
                          nil
                          (.-sender channel)
                          (.-receiver channel))

      (do (a/close! (.-ch_ghost1 channel))
          (a/close! (.-ch channel)))

      (throw (runtime-exception :close nil channel)))))

;;;;
;;;; >!! and <!!
;;;;

(defn >!!
  [channel message]
  {:pre [(channel? channel)]}
  (if (.-buffered channel)

    ;; Buffered channel
    (do (a/>!! (.-ch_ghost1 channel) token)
        (if (monitors/verify! (.-monitor channel)
                              :send
                              message
                              (.-sender channel)
                              (.-receiver channel))

          ;; If ok, commit
          (let [x (a/>!! (.-ch channel) message)
                _ (a/>!! (.-ch_ghost2 channel) token)]
            x)

          ;; If not ok, abort
          (do (a/<!! (.-ch_ghost1 channel))
              (throw (RuntimeException.)))))

    ;; Unbuffered channel
    (do (a/>!! (.-ch_ghost1 channel) token)
        (if (monitors/verify! (.-monitor channel)
                              :sync
                              message
                              (.-sender channel)
                              (.-receiver channel))

          (do (a/>!! (.-ch channel) message))

          (do (a/>!! (.-ch channel) sync-not-ok)
              (throw (RuntimeException.)))))))

(defn <!!
  [channel]
  {:pre [(channel? channel)]}
  (if (.-buffered channel)

    ;; Buffered channel
    (do (a/<!! (.-ch_ghost2 channel))
        (if (monitors/verify! (.-monitor channel)
                              :receive
                              nil
                              (.-sender channel)
                              (.-receiver channel))

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
;;;; put! and take!
;;;;

(defn put!
  [channel message f]
  {:pre [(channel? channel) (fn? f)]}
  (if (.-buffered channel)

    ;; Buffered channel
    (a/put! (.-ch_ghost1 channel)
            token
            (fn [_] (if (monitors/verify! (.-monitor channel)
                                          :send
                                          message
                                          (.-sender channel)
                                          (.-receiver channel))

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
            (fn [_] (if (monitors/verify! (.-monitor channel)
                                          :sync
                                          message
                                          (.-sender channel)
                                          (.-receiver channel))

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
             (fn [_] (if (monitors/verify! (.-monitor channel)
                                           :receive
                                           nil
                                           (.-sender channel)
                                           (.-receiver channel))

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