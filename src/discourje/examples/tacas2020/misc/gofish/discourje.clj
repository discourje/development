(ns discourje.examples.tacas2020.misc.gofish.discourje
  (require [discourje.core.async :refer :all]))

;;
;; Configuration
;;

(discourje.core.logging/set-logging-exceptions)
(discourje.core.logging/set-throwing true)

;;
;; Roles
;;

(def dealer (role "dealer"))
(def player (role "player"))

;;
;; Specification
;;

(def go-fish-i (dsl :i :k
                    (fix [:Play :i]
                         (rep alt [:j (range-without-i :k :i)]
                              [(--> (player :i) (player :j) Card?)
                               (alt
                                 [(--> (player :j) (player :i) Card!) (fix [:Play :i])]
                                 [(--> (player :j) (player :i) GoFish)
                                  (--> (player :i) dealer Fish)
                                  (--> dealer (player :i) Card)])]))))

(def go-fish (dsl :k
                  [(rep seq [_ (range 5)]
                        (ins one-all-one dealer player :k Card Ack))
                   (--> dealer (player 0) Turn)
                   (fix :Game [(rep seq [:i (range :k)]
                                    [(ins go-fish-i :i :k)
                                     (ins succ player :i :k Turn)])
                               (fix :Game)])
                   ]))

;;
;; Implementation
;;

(load "threads")

(def run
  (fn [k]
    (let [barrier (java.util.concurrent.CyclicBarrier. (inc k))
          m (moni (spec (ins go-fish k)))
          dealer->players (vec (for [i (range k)] (chan 5 dealer (player i) m)))
          players->dealer (vec (for [i (range k)] (chan 5 (player i) dealer m)))
          players->players (to-array-2d (for [i (range k)]
                                          (for [j (range k)]
                                            (if (not= i j)
                                              (chan 1 (player i) (player j) m)
                                              nil))))]

      (thread-dealer k dealer->players players->dealer barrier)
      (doseq [i (range k)]
        (thread-player i k dealer->players players->dealer players->players barrier))
      (.await barrier))))

;(try
;  (run 4)
;  (catch Exception e (.printStackTrace e)))