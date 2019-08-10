(ns discourje.examples.experimental.ttt.clojure
  (require [discourje.examples.ttt.ttt :refer :all]
           [discourje.examples.experimental.api :refer [bench join]]
           [clojure.core.async :refer [thread chan >!! <!!]]))

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
  [time]
  (let [board init-board
        a->b (chan 1)
        b->a (chan 1)]
    (bench time #(let [a (thread (alicefn board a->b b->a))
                                   b (thread (bobfn board b->a a->b))]
                               (join [a b])))))

(ttt-discourje 5)