
;; Java dependencies
(import discourje.examples.experimental.chess.Engine)
(set! (. Engine STOCKFISH) "/Users/sung/Desktop/stockfish-10-64")
(set! (. Engine TIME) 1000)
(set! (. Engine MOVES_TO_GO) 1)

;; white
(thread
  (let [e (new Engine)]
    (>!! w->b (. e turn nil))
    (loop []
      (let [m (<!! w<-b)]
        (when (not= m "(none)")
          (let [m (. e turn m)]
            (>!! w->b m)
            (when (not= m "(none)")
              (recur))))))
    (close! w->b)
    (. e kill)))

;; black
(thread
  (let [e (new Engine)]
    (loop []
      (let [m (<!! b<-w)]
        (when (not= m "(none)")
          (let [m (. e turn m)]
            (>!! b->w m)
            (when (not= m "(none)")
              (recur))))))
    (close! b->w)
    (. e kill)))