(def thread-worker
  (fn [i k workers->workers n-iter]
    (cond

      ;; Worker i=0
      (= i 0)
      (thread
        (let [in (nth workers->workers (dec k))
              out (nth workers->workers 0)]
          (doseq [_ (range n-iter)]
            (>!! out i)
            (<!! in))))

      ;; Worker 0<i<=k
      (and (< 0 i) (<= i k))
      (thread
        (let [in (nth workers->workers (dec i))
              out (nth workers->workers i)]
          (doseq [_ (range n-iter)]
            (<!! in)
            (>!! out i))))

      )))

(defn join
  [threads]
  (if (vector? threads)
    (doseq [t threads] (clojure.core.async/<!! t))
    (clojure.core.async/<!! threads)))