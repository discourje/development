(ns discourje.examples.micro.mesh
  (:require [clojure.core.async]
            [discourje.core.async]
            [discourje.core.util :as u]
            [discourje.core.spec :as s]
            [discourje.core.lint :as l]
            [discourje.examples.config :as config]))

;;;;;
;;;;; Specification
;;;;;

(s/defrole ::worker)

(s/defsession ::mesh-unbuffered [k]
  (s/* (s/alt-every [i (range k)
                     j (remove #{i} (range k))]
         (s/--> Boolean (::worker i) (::worker j)))))

(s/defsession ::mesh-buffered [k]
  (s/par-every [i (range k)
                j (remove #{i} (range k))]
    (s/* (s/-->> Boolean (::worker i) (::worker j)))))

(defn spec []
  (condp = (:flags config/*input*)
    #{:unbuffered}
    (mesh-unbuffered (:k config/*input*))
    #{:buffered}
    (mesh-buffered (:k config/*input*))))

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
          mesh
          (cond (contains? flags :unbuffered)
                (u/mesh a/chan (range k))
                (contains? flags :buffered)
                (u/mesh (partial a/chan 1) (range k)))

          ;; Link monitor [optional]
          _
          (if (= config/*run* :dcj)
            (let [m (a/monitor (spec) :n k)]
              (u/link-mesh mesh worker m)))

          ;; Spawn threads
          workers
          (mapv (fn [i] (a/thread (loop [to-put (zipmap (remove #{i} (range k)) (repeat n))
                                         to-take (zipmap (remove #{i} (range k)) (repeat n))]
                                    (let [keep-fn (fn [[j count]] (if (> count 0) j))
                                          puts (u/puts mesh [i true] (keep keep-fn to-put))
                                          takes (u/takes mesh (keep keep-fn to-take) i)
                                          puts-and-takes (into puts takes)]
                                      (if (not-empty puts-and-takes)
                                        (let [[_ c] (a/alts!! puts-and-takes)]
                                          (if (= (u/putter-id mesh c) i)
                                            (recur (update to-put (u/taker-id mesh c) dec) to-take)
                                            (recur to-put (update to-take (u/putter-id mesh c) dec)))))))))
                (range k))

          ;; Await termination
          output
          (doseq [worker workers]
            (a/<!! worker))]

      (set! config/*output* output))))