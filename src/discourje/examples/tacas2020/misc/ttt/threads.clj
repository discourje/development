(def blank " ")
(def cross "x")
(def nought "o")

(def initial-grid [blank blank blank
                   blank blank blank
                   blank blank blank])

(def get-blank (fn [g]
                 (loop [i (long (rand-int 9))]
                   (if (= (nth g i) blank)
                     i
                     (recur (mod (inc i) 9))))))

(def add (fn [g i x-or-o]
           (try (assoc g i x-or-o)
                (catch Exception e (println g i x-or-o) (.printStackTrace e)))))

(def not-final? (fn [g]
                  (and (loop [i 0]
                         (cond (= (nth g i) blank) true
                               (= i 8) false
                               :else (recur (inc i))))
                       (every? #(= false %) (for [l [(set [(nth g 0) (nth g 1) (nth g 2)])
                                                     (set [(nth g 3) (nth g 4) (nth g 5)])
                                                     (set [(nth g 6) (nth g 7) (nth g 8)])
                                                     (set [(nth g 0) (nth g 3) (nth g 6)])
                                                     (set [(nth g 1) (nth g 4) (nth g 7)])
                                                     (set [(nth g 2) (nth g 5) (nth g 8)])
                                                     (set [(nth g 0) (nth g 4) (nth g 8)])
                                                     (set [(nth g 2) (nth g 4) (nth g 6)])]]
                                              (and (= (count l) 1) (not= (first l) blank)))))))

(def print-grid (fn [g]
                  (println "+---+---+---+")
                  (println "|" (nth g 0) "|" (nth g 1) "|" (nth g 2) "|")
                  (println "+---+---+---+")
                  (println "|" (nth g 3) "|" (nth g 4) "|" (nth g 5) "|")
                  (println "+---+---+---+")
                  (println "|" (nth g 6) "|" (nth g 7) "|" (nth g 8) "|")
                  (println "+---+---+---+")))

(def loop-barrier (java.util.concurrent.CyclicBarrier. 2))
(def thread-barrier (java.util.concurrent.CyclicBarrier. 3))

(thread
  (loop [g initial-grid]
    (let [i (get-blank g)
          g (add g i cross)]
      (>!! a->b i)
      (if (not-final? g)
        (let [i (<!! a<-b)
              g (add g i nought)]
          (if (not-final? g)
            (recur g)))
        (print-grid g))))
  (.await loop-barrier)
  (close! a->b)
  (.await thread-barrier))

(thread
  (loop [g initial-grid]
    (let [i (<!! b<-a)
          g (add g i cross)]
      (if (= i nil) (throw (Exception.)))
      (if (not-final? g)
        (let [i (get-blank g)
              g (add g i nought)]
          (>!! b->a i)
          (if (not-final? g)
            (recur g)
            (print-grid g))))))
  (.await loop-barrier)
  (close! b->a)
  (.await thread-barrier))

(.await thread-barrier)