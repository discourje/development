(import discourje.examples.tacas2020.misc.chess.Engine)

(def loop-barrier (java.util.concurrent.CyclicBarrier. 2))
(def thread-barrier (java.util.concurrent.CyclicBarrier. 3))

(thread
  (let [e (Engine. false)]
    (>!! w->b (.turn e nil))
    (loop []
      (let [m (<!! w<-b)]
        (if (not= m "(none)")
          (let [m (.turn e m)]
            (>!! w->b m)
            (if (not= m "(none)")
              (recur))))))
    (.await loop-barrier)
    (close! w->b)
    (.kill e)
    (.await thread-barrier)))

(thread
  (let [e (Engine. true)]
    (loop []
      (let [m (<!! b<-w)]
        (if (not= m "(none)")
          (let [m (.turn e m)]
            (>!! b->w m)
            (if (not= m "(none)")
              (recur))))))
    (.await loop-barrier)
    (close! b->w)
    (.kill e)
    (.await thread-barrier)))

(.await thread-barrier)
