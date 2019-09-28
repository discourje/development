(ns discourje.examples.experimental.ttt.discourje
  (require [discourje.examples.experimental.ttt.ttt :refer :all]
           [discourje.examples.experimental.api :refer :all]
           [discourje.examples.experimental.dsl :refer :all]))

(defn alicefn [board a->b a<-b]
  (let [index (rand-blank-index board)
        board' (play-cross board index)]
    (>!! a->b index)
    (if (not (final? board'))
      (let [index' (<!! a<-b)
            board'' (play-nought board' index')]
        (if (not (final? board''))
          (alicefn board'' a->b a<-b)))
      (output board'))))

(defn bobfn [board b->a b<-a]
  (let [index (<!! b<-a)
        board' (play-cross board index)]
    (if (not (final? board'))
      (let [index' (rand-blank-index board)
            board'' (play-nought board' index')]
        (>!! b->a index')
        (if (not (final? board''))
          (bobfn board'' b->a b<-a)
          (output board''))))))

(defn ttt-discourje
  [time m]
  (let [board init-board
        a->b (chan 1 (alice) (bob) m)
        b->a (chan 1 (bob) (alice) m)]
    (bench time #(let [a (thread (alicefn board a->b b->a))
                       b (thread (bobfn board b->a a->b))]
                   (join [a b]) (monitor-reset m)))))

(def ttt (fix :X [(--> (alice) (bob) Long)
                  (--> (bob) (alice) Long)
                  (fix :X)]))

(ttt-discourje 5 (monitor (spec ttt)))