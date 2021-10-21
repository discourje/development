(ns discourje.examples.micro.star
  (:require [clojure.core.async]
            [discourje.core.async]
            [discourje.core.util :as u]
            [discourje.core.spec :as s]
            [discourje.core.lint :as l]
            [discourje.examples.config :as config]))

(config/clj-or-dcj)

;;;;;
;;;;; Specification
;;;;;

(s/defrole ::master)
(s/defrole ::worker)

(s/defsession ::star-unbuffered-outwards [k]
  (s/* (s/alt-every [i (range k)]
         (s/--> Boolean ::master (::worker i)))))

(s/defsession ::star-unbuffered-inwards [k]
  (s/* (s/alt-every [i (range k)]
         (s/--> Boolean (::worker i) ::master))))

(s/defsession ::star-buffered-outwards [k]
  (s/par-every [i (range k)]
    (s/* (s/-->> Boolean ::master (::worker i)))))

(s/defsession ::star-buffered-inwards [k]
  (s/par-every [i (range k)]
    (s/* (s/-->> Boolean (::worker i) ::master))))

(defn spec []
  (condp = (:flags config/*input*)
    #{:unbuffered :outwards}
    (star-unbuffered-outwards (:k config/*input*))
    #{:unbuffered :inwards}
    (star-unbuffered-inwards (:k config/*input*))
    #{:buffered :outwards}
    (star-buffered-outwards (:k config/*input*))
    #{:buffered :inwards}
    (star-buffered-inwards (:k config/*input*))))

(when (some? config/*lint*)
  (set! config/*output* (l/lint (spec))))

;;;;;
;;;;; Implementation
;;;;;

(config/clj-or-dcj)

(when (some? config/*run*)
  (let [input config/*input*
        flags (:flags input)
        k (:k input)
        n (:n input)]

    (let [;; Create channels
          star
          (cond
            (contains? flags :unbuffered)
            (u/star a/chan nil (range k))
            (contains? flags :buffered)
            (u/star (partial a/chan 1) nil (range k)))

          ;; Link monitor [optional]
          _
          (if (= config/*run* :dcj)
            (let [m (a/monitor (spec))]
              (u/link-star star master worker m)))

          ;; Spawn threads
          master
          (a/thread (cond
                      (contains? flags :outwards)
                      (a/thread (loop [to-put (zipmap (range k) (repeat n))]
                                  (let [keep-fn (fn [[j count]] (if (> count 0) j))
                                        puts (u/puts star [nil true] (keep keep-fn to-put))]
                                    (if (not-empty puts)
                                      (let [[_ c] (a/alts!! puts)]
                                        (recur (update to-put (u/taker-id star c) dec)))))))
                      (contains? flags :inwards)
                      (a/thread (loop [to-take (zipmap (range k) (repeat n))]
                                  (let [keep-fn (fn [[j count]] (if (> count 0) j))
                                        takes (u/takes star (keep keep-fn to-take) nil)]
                                    (if (not-empty takes)
                                      (let [[_ c] (a/alts!! takes)]
                                        (recur (update to-take (u/putter-id star c) dec)))))))))

          workers
          (mapv (fn [i] (cond
                          (contains? flags :outwards)
                          (a/thread (doseq [_ (range n)]
                                      (a/<!! (star nil i))))
                          (contains? flags :inwards)
                          (a/thread (doseq [_ (range n)]
                                      (a/>!! (star i nil) true)))))
                (range k))

          ;; Await termination
          output
          (do (a/<!! master)
              (doseq [worker workers]
                (a/<!! worker)))]

      (set! config/*output* output))))
