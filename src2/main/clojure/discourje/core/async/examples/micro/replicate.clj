(ns discourje.core.async.examples.micro.replicate
  (:require [clojure.core.async]
            [discourje.core.async :as dcj]
            [discourje.core.async.examples.config :as config]
            [discourje.spec :as s]))

(if (contains? (ns-aliases *ns*) 'a)
  (ns-unalias *ns* 'a))

(case config/*lib*
  :clj (alias 'a 'clojure.core.async)
  :dcj (alias 'a 'discourje.core.async)
  :dcj-nil (alias 'a 'discourje.core.async)
  nil)

;;;;
;;;; Specification
;;;;

(s/defrole ::sender "sender")
(s/defrole ::receiver "receiver")

(s/def ::replicate
  [k]
  (s/* (s/loop replicate [i 0]
               (s/if (< i k)
                 (s/par (s/--> Boolean ::sender (::receiver i))
                        (s/recur replicate (inc i)))))))

;;;;
;;;; Implementation
;;;;

(let [input config/*input*
      k (:k input)
      secs (:secs input)]

  (let [;; Start timer
        begin (System/nanoTime)

        ;; Create channels
        sender->receivers
        (mapv (fn [_] (a/chan)) (range k))

        ;; Link monitor [optional]
        _
        (if (= config/*lib* :dcj)
          (let [s (s/apply ::replicate [k])
                m (dcj/monitor s)]
            (doseq [i (range k)] (dcj/link (nth sender->receivers i)
                                           (s/role ::sender)
                                           (s/role ::receiver [i])
                                           m))))

        ;; Spawn threads
        sender
        (a/thread (let [begin (System/nanoTime)
                        deadline (+ begin (* secs 1000 1000 1000))]
                    (loop [n-iter 0]
                      (if (< (System/nanoTime) deadline)
                        (do (doseq [out sender->receivers]
                              (a/>!! out true))
                            (recur (inc n-iter)))
                        (do (doseq [out sender->receivers]
                              (a/>!! out false))
                            [(- (System/nanoTime) begin) n-iter])))))
        receivers
        (mapv #(a/thread (let [in (nth sender->receivers %)]
                           (loop []
                             (if (a/<!! in)
                               (recur)))))
              (range k))

        ;; Await termination
        output
        (do (doseq [receiver receivers]
              (a/<!! receiver))
            (a/<!! sender))

        ;; Stop timer
        end (System/nanoTime)]

    (set! config/*output* output)
    (set! config/*time* (- end begin))))
