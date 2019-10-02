(def thread-worker
  (fn [[i k] chans n-iter]
    (cond

      ;; Worker i=0
      (= i 0)
      (thread
        (let [in (get chans (dec k))
              out (get chans 0)]
          (doseq [_ (range n-iter)]
            (>!! out i)
            (<!! in))))

      ;; Worker 0<i<=k
      (and (< 0 i) (<= i k))
      (thread
        (let [in (get chans (dec i))
              out (get chans i)]
          (doseq [_ (range n-iter)]
            (<!! in)
            (>!! out i))))

      )))