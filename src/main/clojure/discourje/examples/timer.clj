(ns discourje.examples.timer
  (:refer-clojure :exclude [update longs quot]))

;;;;
;;;; Stats
;;;;

(defrecord Stats [n big-m big-s x-min x-max])

(defn stats []
  (->Stats 0 nil nil nil nil))

;; References:
;;  * Knuth: The art of computer programming, Volume II: Seminumerical Algorithms, 3rd Edition. Addison-Wesley 1998. (§4.2.2)
;;  * Hoefler, Belli: Scientific benchmarking of parallel computing systems: twelve ways to tell the masses when reporting
;;    performance results. SC 2015.

(defn update [stats x']
  (let [n (:n stats)
        big-m (:big-m stats)]
    (if (= n 0)
      (assoc stats :n 1 :big-m x' :big-s 0 :x-min x' :x-max x')
      (let [n' (inc n)
            big-m' (+ big-m (/ (- x' big-m) n'))
            big-s' (+ (:big-s stats) (* (- x' big-m) (- x' big-m')))
            x-min' (min (:x-min stats) x')
            x-max' (max (:x-max stats) x')]
        (assoc stats :n n' :big-m big-m' :big-s big-s' :x-min x-min' :x-max x-max')))))

;; References:
;;  * Salkind: Encyclopedia of Research Design, Volume I. SAGE Publications 2010. (Central Tendency, Measures of)
;;  * Salkind: Encyclopedia of Research Design, Volume II. SAGE Publications 2010. (Pooled Variance)

(defn pool [& statss]
  (if (empty? statss)
    nil
    (let [n (reduce + (map #(:n %) statss))]
      (assoc (stats) :n n
                     :big-m (if (> n 0) (reduce + (map #(* (/ (:n %) n) (:big-m %)) statss)))
                     :big-s (if (> n (count statss))
                              (/ (reduce + (map #(* (dec (:n %)) (:big-s %)) statss)) (- n (count statss))))
                     :x-min (apply min (map #(:x-min %) statss))
                     :x-max (apply max (map #(:x-max %) statss))))))

;;;;
;;;; Timer
;;;;

(defrecord Timer [resolution ticks stats nanos aggregate])

(defn timer [resolution]
  (->Timer resolution 0 (stats) (System/nanoTime) false))

(defn tick [t]
  {:pre [(not (:aggregate t))]}
  (let [ticks' (inc (:ticks t))]
    (if (= 0 (mod ticks' (:resolution t)))
      (assoc t :ticks ticks'
               :stats (update (:stats t) (- (System/nanoTime) (:nanos t)))
               :nanos (System/nanoTime))
      (assoc t :ticks ticks'))))

(defn aggregate [& timers]
  {:pre [(not (empty? timers)) (every? #(= (:resolution %) (:resolution (first timers))) (rest timers))]}
  (assoc (timer (:resolution (first timers))) :ticks (reduce + (map #(:ticks %) timers))
                                              :stats (apply pool (mapv #(:stats %) timers))
                                              :nanos nil
                                              :aggregate true))

(defn report [t]
  (let [stats (:stats t)]
    {:ticks     (:ticks t)
     :stats     {:n     (:n stats)
                 :μ-hat (long (:big-m stats))
                 :σ-hat (if (and (:bis-s stats) (> (:n stats) 1))
                          (long (Math/sqrt (/ (:big-s stats) (dec (:n stats))))))
                 :x-min (:x-min stats)
                 :x-max (:x-max stats)}
     :aggregate (:aggregate t)}))
