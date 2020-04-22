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

(defn- throw-runtime-exception [type message channel]
  (throw (ex-info (str "[SESSION FAILURE] Action "
                       (case type :sync "‽" :send "!" :receive "?" :close "C" (throw (Exception.)))
                       "("
                       (if (nil? message) "" (str message ","))
                       (.-sender channel)
                       ","
                       (.-receiver channel)
                       ") is not enabled in current state(s): "
                       (monitors/str-current-states (.-monitor channel))
                       ". LTS in Aldebaran format (http://cadp.inria.fr/man/aut.html):\n\n"
                       (monitors/str-lts (.-monitor channel))
                       "\n")
                  {:message message
                   :channel channel})))

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

      (throw-runtime-exception :close nil channel))

    ;; Unbuffered channel
    (if (monitors/verify! (.-monitor channel)
                          :close
                          nil
                          (.-sender channel)
                          (.-receiver channel))

      (do (a/close! (.-ch_ghost1 channel))
          (a/close! (.-ch channel)))

      (throw-runtime-exception :close nil channel))))

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
          (throw-runtime-exception :send message channel)))

    ;; Unbuffered channel
    (if (monitors/verify! (.-monitor channel)
                          :sync
                          message
                          (.-sender channel)
                          (.-receiver channel))

      (do (a/>!! (.-ch channel) message))

      (do (a/>!! (.-ch channel) sync-not-ok)
          (throw-runtime-exception :sync message channel)))))

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
          (throw-runtime-exception :receive nil channel)))

    ;; Unbuffered channel
    (let [message (a/<!! (.-ch channel))]
      (if (= message sync-not-ok)
        (throw-runtime-exception :sync nil channel)
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
;(defn put!
;  [channel message f]
;  {:pre [(channel? channel) (fn? f)]}
;  (if (.-buffered channel)
;
;    ;; Buffered channel
;    (a/put! (.-ch_ghost1 channel)
;            token
;            (fn [_] (if (monitors/verify! (.-monitor channel)
;                                          :send
;                                          message
;                                          (.-sender channel)
;                                          (.-receiver channel))
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
;            (fn [_] (if (monitors/verify! (.-monitor channel)
;                                          :sync
;                                          message
;                                          (.-sender channel)
;                                          (.-receiver channel))
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
;             (fn [_] (if (monitors/verify! (.-monitor channel)
;                                           :receive
;                                           nil
;                                           (.-sender channel)
;                                           (.-receiver channel))
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