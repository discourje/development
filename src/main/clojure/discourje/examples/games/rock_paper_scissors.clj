(ns discourje.examples.games.rock-paper-scissors
  (:require [clojure.core.async]
            [discourje.core.async]
            [discourje.core.util :as u]
            [discourje.core.spec :as s]
            [discourje.core.lint :as l]
            [discourje.examples.config :as config]))

;;;;;
;;;;; Specification
;;;;;

(s/defrole ::player)

(s/defsession ::rock-paper-scissors [ids]
  (::rock-paper-scissors-round ids s/empty-set))

(s/defsession ::rock-paper-scissors-round [ids co-ids]
  (s/if (> (s/count ids) 1)
    (s/cat (s/par-every [i ids
                         j (s/disj ids i)]
             (s/--> String (::player i) (::player j)))
           (s/alt-every [winner-ids (s/power-set ids)]
             (s/let [loser-ids (s/difference ids winner-ids)]
               (s/par (::rock-paper-scissors-round winner-ids (s/union co-ids loser-ids))
                      (s/par-every [i loser-ids
                                    j (s/disj (s/union ids co-ids) i)]
                        (s/close (::player i) (::player j)))))))))

(defn spec []
  (rock-paper-scissors (set (range (:k config/*input*)))))

(when (some? config/*lint*)
  (set! config/*output* (l/lint (spec))))

;;;;;
;;;;; Implementation
;;;;;

(config/clj-or-dcj)

(def rock "rock")
(def paper "paper")
(def scissors "scissors")

(defn rock-or-paper-or-scissors []
  (rand-nth [rock paper scissors]))

(defn beats [x y]
  (contains? #{[rock scissors]
               [paper rock]
               [scissors paper]}
             [x y]))

(defn winner-ids [round]
  (let [items (distinct (vals round))
        winning-items (if (= 1 (count items))
                        items
                        (keep identity (for [x items
                                             y items]
                                         (if (beats x y) x nil))))]
    (keep (fn [[i item]] (if (some #{item} winning-items) i)) round)))

(defn winner? [round i]
  (let [winners (winner-ids round)]
    (and (some #{i} winners) (= (count winners) 1))))

(defn loser? [round i]
  (let [winners (winner-ids round)]
    (and (not (some #{i} winners)) (>= (count winners) 1))))

(defn winner-or-loser? [round i]
  (or (winner? round i) (loser? round i)))

(defn println-rounds [rounds]
  (println)
  (doseq [[i m] (map-indexed #(vector (inc %1) %2) rounds)]
    (println (str "Round " i ": " (into (sorted-map) m))))
  (println))

(when (some? config/*run*)
  (let [input config/*input*
      k (:k input)]

  (let [;; Create channels
        players<->players
        (u/mesh a/chan (range k))

        ;; Create barrier
        barrier
        (java.util.concurrent.Phaser. k)

        ;; Link monitor [optional]
        _
        (if (= config/*run* :dcj)
          (let [m (a/monitor (spec) :n k)]
            (u/link-mesh players<->players player m)))

        ;; Spawn threads
        players
        (mapv (fn [i] (a/thread (loop [ids (range k)
                                       rounds []]

                                  (let [item (rock-or-paper-or-scissors)
                                        opponent-ids (remove #{i} ids)
                                        round (loop [acts (into (u/puts players<->players [i item] opponent-ids)
                                                                (u/takes players<->players opponent-ids i))
                                                     round {}]
                                                (if (empty? acts)
                                                  (assoc round i item)
                                                  (let [[v c] (a/alts!! acts)]
                                                    (recur (remove #{[c item] c} acts)
                                                           (assoc round (u/putter-id players<->players c) v)))))]

                                    (.arriveAndAwaitAdvance barrier)

                                    (if (winner? round i)
                                      (println-rounds (conj rounds round)))

                                    (if (winner-or-loser? round i)
                                      (do (.arriveAndDeregister barrier)
                                          (doseq [j (remove #{i} (range k))]
                                            (a/close! (players<->players i j))))
                                      (recur (winner-ids round) (conj rounds round)))))))
              (range k))

        ;; Await termination
        output
        (doseq [i (range k)]
          (a/<!! (nth players i)))]

    (set! config/*output* output))))