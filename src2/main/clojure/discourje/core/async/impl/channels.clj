(ns discourje.core.async.impl.channels
  (:require [clojure.core.async :as a]
            [discourje.core.async.impl.ast :as ast]
            [discourje.core.async.impl.monitors :as monitors]))

(deftype Channel [sender receiver ch ch-ghost1 ch-ghost2 closed? monitor])

(defn channel? [x]
  {:pre [true]}
  (= (type x) Channel))

(defn channel [n sender receiver monitor]
  {:pre [(number? n) (> n 0)
         (ast/role? sender)
         (ast/role? receiver)
         (monitors/monitor? monitor)]}
  (->Channel (ast/eval-role sender)
             (ast/eval-role receiver)
             (a/chan n)
             (a/chan n)
             (a/chan n)
             false
             monitor))

(def token 0)

;;;;
;;;; put! and take!
;;;;

(defn put!
  ([channel message f]
   (a/put! (.-ch_ghost1 channel)
           token
           #(do %
                (if (monitors/verify-send! message
                                           (.-sender channel)
                                           (.-receiver channel)
                                           (.-monitor channel))
                  ;; If ok, commit
                  (do
                    (a/put! (.-ch channel) message f)
                    (a/put! (.-ch_ghost2 channel) token f))

                  ;; If not ok, rollback
                  (do
                    (a/take! (.-ch_ghost1 channel) identity)
                    (throw (RuntimeException.))))))))

(defn take!
  ([channel f]
   (a/take! (.-ch_ghost2 channel)
            #(do %
                 (if (monitors/verify-receive! (.-sender channel)
                                               (.-receiver channel)
                                               (.-monitor channel))
                   ;; If ok, commit
                   (do
                     (a/take! (.-ch channel) f)
                     (a/take! (.-ch_ghost1 channel) f))

                   ;; If not ok, rollback
                   (do
                     (a/put! (.-ch_ghost2 channel) token)
                     (throw (RuntimeException.))))))))