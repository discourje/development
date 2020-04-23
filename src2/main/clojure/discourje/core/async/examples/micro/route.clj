(ns discourje.core.async.examples.micro.route
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

(s/def ::route
  [k]
  (s/* (s/loop route [i 0]
               (s/if (< i k)
                 (s/alt (s/--> Boolean ::sender (::receiver i))
                        (s/recur route (inc i)))))))

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
          (let [s (s/apply ::route [k])
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
                        (do (a/alts!! (mapv (fn [out] [out true]) sender->receivers))
                            (recur (inc n-iter)))
                        (do (loop [todo sender->receivers]
                              (if (not (empty? todo))
                                (let [[_ c] (a/alts!! (mapv (fn [out] [out false]) todo))]
                                  (recur (remove #(= c %) todo)))))
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
