(def thread-master
  (fn [k master->workers workers->master n-iter]
    (thread
      (doseq [_ (range n-iter)]
        (doseq [i (range k)]
          (>!! (nth master->workers i) 0)
          (<!! (nth workers->master i)))))))

(def thread-worker
  (fn [i master->workers workers->master n-iter]
    (thread
      (doseq [_ (range n-iter)]
        (<!! (nth master->workers i))
        (>!! (nth workers->master i) 0)))))

(defn join
  [threads]
  (if (vector? threads)
    (doseq [t threads] (clojure.core.async/<!! t))
    (clojure.core.async/<!! threads)))