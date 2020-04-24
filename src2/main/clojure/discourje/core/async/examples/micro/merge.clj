(ns discourje.core.async.examples.micro.merge
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

(s/defrole ::sender "senders")
(s/defrole ::receiver "receiver")

(s/def ::merge
  [k]
  (s/* (s/loop merge [i 0]
                      (s/if (< i k)
                        (s/alt (s/--> Boolean (::sender i) ::receiver)
                               (s/recur merge (inc i)))))))

;;;;
;;;; Implementation
;;;;

(let [input config/*input*
      k (:k input)
      secs (:secs input)]

  (let [;; Start timer
        begin (System/nanoTime)

        ;; Create channels
        senders->receiver
        (mapv (fn [_] (a/chan)) (range k))

        ;; Link monitor [optional]
        _
        (if (= config/*lib* :dcj)
          (let [s (s/apply ::merge [k])
                m (dcj/monitor s)]
            (doseq [i (range k)] (dcj/link (nth senders->receiver i)
                                           (s/role ::sender [i])
                                           (s/role ::receiver)
                                           m))))

        ;; Spawn threads
        senders
        (mapv #(a/thread (let [out (nth senders->receiver %)
                               begin (System/nanoTime)
                               deadline (+ begin (* secs 1000 1000 1000))]
                           (loop []
                             (if (< (System/nanoTime) deadline)
                               (do (a/>!! out true)
                                   (recur))
                               (do (a/>!! out false))))))
              (range k))

        receiver
        (a/thread (let [begin (System/nanoTime)]
                    (loop [n-iter 0
                           ins senders->receiver]
                      (if (empty? ins)
                        [(- (System/nanoTime) begin) n-iter]
                        (let [[v in] (a/alts!! ins)]
                          (if v
                            (recur (inc n-iter) ins)
                            (recur (inc n-iter) (remove #(= in %) ins))))))))

        ;; Await termination
        output
        (do (doseq [sender senders]
              (a/<!! sender))
            (a/<!! receiver))

        ;; Stop timer
        end (System/nanoTime)]

    (set! config/*output* output)
    (set! config/*time* (- end begin))))
