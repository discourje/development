(ns discourje.examples.clojured22.rps)

(def MOVES [:rock :paper :scissors])
(def BEATS {:rock :scissors, :paper :rock, :scissors :paper})

(defn winner [[name1 move1] [name2 move2]]
  (cond
    (= move1 move2) "no one"
    (= move2 (BEATS move1)) name1
    :else name2))

(defn report [winner]
  (println)
  (println winner "wins!"))