(ns discourje.examples.experimental.ttt.ttt)

(def blank " ")
(def nought "O")
(def cross "X")

(def init-board [blank blank blank
                 blank blank blank
                 blank blank blank])

(defn blank-indices [board]
  (map first (filter (fn [x] (= blank (second x))) (map-indexed vector board))))
(defn rand-blank-index [board]
  (rand-nth (blank-indices board)))

(defn winner? [board nought-or-cross]
  (or
    ;; horizontal
    (= (get board 0) (get board 1) (get board 2) nought-or-cross)
    (= (get board 3) (get board 4) (get board 5) nought-or-cross)
    (= (get board 6) (get board 7) (get board 8) nought-or-cross)
    ;; vertical
    (= (get board 0) (get board 3) (get board 6) nought-or-cross)
    (= (get board 1) (get board 4) (get board 7) nought-or-cross)
    (= (get board 2) (get board 5) (get board 8) nought-or-cross)
    ;; diagonal
    (= (get board 0) (get board 4) (get board 8) nought-or-cross)
    (= (get board 2) (get board 4) (get board 6) nought-or-cross)))

(defn final? [board]
  (or (empty? (blank-indices board)) (winner? board nought) (winner? board cross)))

(defn play [board index nought-or-cross]
  (assoc board index nought-or-cross))
(defn play-nought [board index]
  (play board index nought))
(defn play-cross [board index]
  (play board index cross))

(def output? (atom false))

(defn output [board]
  (when @output?
    (println "" (get board 0) "|" (get board 1) "|" (get board 2))
    (println "---+---+---")
    (println "" (get board 3) "|" (get board 4) "|" (get board 5))
    (println "---+---+---")
    (println "" (get board 6) "|" (get board 7) "|" (get board 8))))