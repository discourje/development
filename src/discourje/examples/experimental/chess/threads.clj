;; Java dependencies
(import discourje.examples.experimental.chess.Engine)
(set! (. Engine STOCKFISH) "/Users/sung/Desktop/stockfish-10-64")
(set! (. Engine TIME) 1000)
(set! (. Engine MOVES_TO_GO) 1)

;; White
(def thread-white
  (thread
    (let [e (new Engine false)]
      (>!! w->b (. e turn nil))
      (loop []
        (let [m (<!! w<-b)]
          (when (not= m "(none)")
            (let [m (. e turn m)]
              (>!! w->b m)
              (when (not= m "(none)")
                (recur))))))
      (Thread/sleep 1000)
      (close! w->b)
      (println "White done")
      (. e kill))))

;; Black
(def thread-black
  (thread
    (let [e (new Engine true)]
      (loop []
        (let [m (<!! b<-w)]
          (when (not= m "(none)")
            (let [m (. e turn m)]
              (>!! b->w m)
              (when (not= m "(none)")
                (recur))))))
      (Thread/sleep 1000)
      (close! b->w)
      (println "Black done")
      (. e kill))))

;; Join
(discourje.examples.experimental.util/join [thread-white thread-black])