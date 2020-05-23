(ns discourje.core.async.examples.games.rock-paper-scissors
  (:require [clojure.core.async]
            [discourje.core.async]
            [discourje.core.util :as u]
            [discourje.core.async.examples.config :as config]
            [discourje.spec :as s]))

(config/clj-or-dcj)

;;;;
;;;; Specification
;;;;

(s/defrole ::player)

(s/defsession ::rock-paper-scissors [player-ids]
  (::rock-paper-scissors-round player-ids #{}))

(s/defsession ::rock-paper-scissors-round [player-ids non-player-ids]
  (s/if (not (empty? player-ids))
    (s/alt-every [winner-ids (s/power-set player-ids)]
      (s/let [loser-ids (s/difference player-ids winner-ids)]
        (s/cat (s/par-every [i player-ids
                             j (disj player-ids i)]
                 (s/--> String (::player i) (::player j)))
               (s/par (s/par-every [i loser-ids
                                    j (disj (s/union player-ids non-player-ids) i)]
                        (s/close (::player i) (::player j)))
                      (::rock-paper-scissors-round winner-ids (s/union non-player-ids loser-ids))))))))

;;;;
;;;; Implementation
;;;;

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

(defn winners [round]
  (let [items (distinct (vals round))
        winning-items (if (= 1 (count items))
                        items
                        (keep identity (for [x items
                                             y items]
                                         (if (beats x y) x nil))))]
    (keep (fn [[i item]] (if (some #{item} winning-items) i)) round)))

(defn winner? [round i]
  (let [winners (winners round)]
    (and (some #{i} winners) (= (count winners) 1))))

(defn loser? [round i]
  (let [winners (winners round)]
    (and (not (some #{i} winners)) (>= (count winners) 1))))

(defn winner-or-loser? [round i]
  (or (winner? round i) (loser? round i)))

(defn println-rounds [rounds]
  (println)
  (doseq [[i m] (map-indexed #(vector (inc %1) %2) rounds)]
    (println (str "Round " i ": " (into (sorted-map) m))))
  (println))

(let [input config/*input*
      _ (:resolution input)
      k (:k input)]

  (let [;; Start timer
        begin (System/nanoTime)

        ;; Create channels
        players<->players
        (u/mesh a/chan (range k))

        ;; Create barrier
        barrier
        (java.util.concurrent.Phaser. k)

        ;; Link monitor [optional]
        _
        (if (= config/*lib* :dcj)
          (let [s (rock-paper-scissors (set (range k)))
                m (a/monitor s)]
            (u/link-all players<->players player m)))

        ;; Spawn threads
        players
        (mapv (fn [i] (a/thread (loop [player-ids (range k)
                                       rounds []]

                                  (let [item (rock-or-paper-or-scissors)
                                        opponent-ids (remove #{i} player-ids)
                                        round (loop [actions (into (u/puts players<->players [i item] opponent-ids)
                                                                   (u/takes players<->players opponent-ids i))
                                                     round {}]
                                                (if (empty? actions)
                                                  (assoc round i item)
                                                  (let [[v c] (a/alts!! actions)]
                                                    (recur (remove #{[c item] c} actions)
                                                           (assoc round (u/putter-id players<->players c) v)))))]

                                    (.arriveAndAwaitAdvance barrier)

                                    (if (winner? round i)
                                      (println-rounds (conj rounds round)))

                                    (if (winner-or-loser? round i)
                                      (do (.arriveAndDeregister barrier)
                                          (doseq [j (remove #{i} (range k))]
                                            (a/close! (players<->players i j))))
                                      (recur (winners round) (conj rounds round)))))))
              (range k))

        ;; Await termination
        output
        (doseq [i (range k)]
          (a/<!! (nth players i)))

        ;; Stop timer
        end (System/nanoTime)]

    (set! config/*output* output)
    (set! config/*time* (- end begin))))