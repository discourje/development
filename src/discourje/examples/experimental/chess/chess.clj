;(ns discourje.examples.experimental.chess.chess
;  (require [clojure.core.async :refer [thread chan >!! <!! close!]]))

(ns discourje.examples.experimental.chess.chess
  (require [discourje.examples.experimental.dsl :refer :all]
           [discourje.examples.experimental.api :refer :all]))

(import discourje.examples.experimental.chess.Engine)

(def white (role "white"))
(def black (role "black"))

(def s (fix :X [(--> white black String)
                (--> black white String)
                (fix :X)]))

(def m (monitor (spec s)))

;; Channels
(def w->b (chan 1 (white) (black) m))
(def b<-w w->b)
(def b->w (chan 1 (black) (white) m))
(def w<-b b->w)

;; White
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
    ;(close! w->b)
    (. e kill)))

;; Black
(thread
  (let [e (new Engine)]
    (loop []
      (let [m (<!! b<-w)]
        (when (not= m "(none)")
          (let [m (. e turn m)]
            (>!! b->w m)
            (when (not= m "(none)")
              (recur))))))
    ;(close! b->w)
    (. e kill)))