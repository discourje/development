(ns discourje.examples.games.go-fish
  (:require [clojure.core.async]
            [discourje.core.async]
            [discourje.core.util :as u]
            [discourje.core.spec :as s]
            [discourje.examples.config :as config]))

(config/clj-or-dcj)

;;;;
;;;; Data types
;;;;

(defrecord Card [suit rank]
  Object
  (toString [_] (str suit rank)))

(defrecord Turn [])
(defrecord Ask [suit rank])
(defrecord Go [])
(defrecord Fish [])

(defrecord OutOfCards [])

;;;;
;;;; Specification
;;;;

(s/defrole ::dealer)
(s/defrole ::player)

(s/defsession ::go-fish [player-ids]
  (s/cat (s/par-every [i player-ids]
           (s/cat-every [_ (range 5)]
             (s/--> Card ::dealer (::player i))))
         (s/alt-every [i player-ids]
           (s/cat (s/--> Turn ::dealer (::player i))
                  (::go-fish-turn i player-ids)))
         (s/par-every [i player-ids]
           (s/cat (s/close ::dealer (::player i))
                  (s/par (s/cat (s/* (s/--> Card (::player i) ::dealer))
                                (s/close (::player i) ::dealer))
                         (s/par-every [j (disj player-ids i)]
                           (s/close (::player i) (::player j))))))))

(s/defsession ::go-fish-turn [i player-ids]
  (s/alt-every [j (disj player-ids i)]
    (s/cat (s/--> Ask (::player i) (::player j))
           (s/alt (s/cat (s/--> Card (::player j) (::player i))
                         (s/--> OutOfCards (::player i) ::dealer))
                  (s/cat (s/--> Card (::player j) (::player i))
                         (::go-fish-turn i player-ids))
                  (s/cat (s/--> Go (::player j) (::player i))
                         (s/--> Fish (::player i) ::dealer)
                         (s/alt (s/--> Card ::dealer (::player i))
                                (s/--> OutOfCards ::dealer (::player i)))
                         (s/--> Turn (::player i) (::player j))
                         (::go-fish-turn j player-ids))))))

;;;;
;;;; Implementation
;;;;

(def spade "\u2660")
(def heart "\u2661")
(def diamond "\u2662")
(def club "\u2663")

(def suits [spade heart diamond club])
(def ranks ["2" "3" "4" "5" "6" "7" "8" "9" "10" "J" "Q" "K" "A"])

(defn comparator-cards [c1 c2]
  (if (= (.indexOf ranks (:rank c1)) (.indexOf ranks (:rank c2)))
    (- (.indexOf suits (:suit c1)) (.indexOf suits (:suit c2)))
    (- (.indexOf ranks (:rank c1)) (.indexOf ranks (:rank c2)))))

(def deck (for [suit suits
                rank ranks]
            (->Card suit rank)))

(defn turn [] (->Turn))
(defn ask [suit rank] (->Ask suit rank))
(defn go [] (->Go))
(defn fish [] (->Fish))
(defn out-of-cards [] (->OutOfCards))

(defn shuffled-deck []
  (shuffle deck))

(defn groups [hand]
  (loop [cards hand
         m {}]
    (if (empty? cards)
      (vals m)
      (let [c (first cards)]
        (recur (rest cards)
               (update m (:rank c) #(if % (conj % c) #{c})))))))

(defn finished-groups [hand]
  (filter #(= (count %) 4) (groups hand)))

(defn unfinished-groups [hand]
  (remove #(= (count %) 4) (groups hand)))

(defn suit-and-rank-to-ask [hand]
  (if-let [g (rand-nth (unfinished-groups hand))]
    [(rand-nth (remove (fn [suit] (some #(= (:suit %) suit) g)) suits))
     (:rank (first g))]))

(defn empty-hand? [hand]
  (empty? (unfinished-groups hand)))

(defn println-hands [hands]
  (println)
  (doseq [[player-id hand] hands]
    (println (str "Player " player-id " (" (count hand) " cards, " (count (finished-groups hand)) ":" (count (unfinished-groups hand)) " groups): "
                  (clojure.string/join " " (map #(str %) (sort comparator-cards hand))))))
  (println))

(let [input config/*input*
      _ (:resolution input)
      k (:k input)]

  (let [;; Start timer
        begin (System/nanoTime)

        ;; Create channels
        dealer<->players
        (u/star a/chan nil (range k))
        players<->players
        (u/mesh a/chan (range k))

        ;; Link monitor [optional]
        _
        (if (= config/*lib* :dcj)
          (let [s (go-fish (set (range k)))
                m (a/monitor s)]
            (u/link-star dealer<->players dealer player m)
            (u/link-mesh players<->players player m)))

        ;; Spawn threads
        dealer
        (a/thread (let [player-ids (range k)
                        deck (shuffled-deck)]

                    ;; Deal initial hands
                    (doseq [i player-ids]
                      (doseq [card (take 5 (drop (* i 5) deck))]
                        (a/>!! (dealer<->players nil i) card)))

                    ;; Pass turn to (randomly selected) first player
                    (a/>!! (dealer<->players nil (rand-nth player-ids)) (turn))

                    ;; Deal cards (upon request) until the deck runs outs
                    (let [deck (loop [deck (drop (* k 5) deck)]
                                 (let [acts (u/takes dealer<->players player-ids nil)
                                       [v c] (a/alts!! acts)
                                       player-id (u/putter-id dealer<->players c)]
                                   (condp = (type v)
                                     Fish (do (a/>!! (dealer<->players nil player-id)
                                                     (if (empty? deck)
                                                       (out-of-cards)
                                                       (first deck)))
                                              (recur (rest deck)))
                                     OutOfCards deck)))]

                      ;; Close channels (game over)
                      (doseq [i player-ids]
                        (a/close! (dealer<->players nil i)))

                      ;; Get final hands
                      (loop [acts (u/takes dealer<->players player-ids nil)
                             hands (into {} (map #(vector % (list)) player-ids))]
                        (if (empty? acts)
                          (println-hands hands)
                          (let [[v c] (a/alts!! acts)
                                player-id (u/putter-id dealer<->players c)]
                            (condp = (type v)
                              Card (recur acts (update hands player-id #(cons v %)))
                              nil (recur (remove #{c} acts) hands))))))))

        players
        (mapv (fn [i] (a/thread (let [opponent-ids (remove #{i} (range k))
                                      hand (doall (for [_ (range 5)]
                                                    (a/<!! (dealer<->players nil i))))]

                                  (loop [hand hand]
                                    (let [acts (into (u/takes dealer<->players [nil] i)
                                                     (u/takes players<->players opponent-ids i))
                                          [v c] (a/alts!! acts)]

                                      (condp = (type v)
                                        Turn (recur (loop [hand hand]
                                                      (let [[suit rank] (suit-and-rank-to-ask hand)
                                                            j (rand-nth opponent-ids)]
                                                        (a/>!! (players<->players i j) (ask suit rank))
                                                        (let [v (a/<!! (players<->players j i))]
                                                          (condp = (type v)
                                                            Card (let [hand (cons v hand)]
                                                                   (if (or (empty-hand? hand) (:last (meta v)))
                                                                     (do (a/>!! (dealer<->players i nil) (out-of-cards))
                                                                         hand)
                                                                     (recur hand)))

                                                            Go (do (a/>!! (dealer<->players i nil) (fish))
                                                                   (let [v (a/<!! (dealer<->players nil i))]
                                                                     (a/>!! (players<->players i j) (turn))
                                                                     (condp = (type v)
                                                                       Card (cons v hand)
                                                                       OutOfCards hand))))))))

                                        Ask (let [j (u/putter-id players<->players c)
                                                  card (first (filter #(and (= (:rank v) (:rank %))
                                                                            (= (:suit v) (:suit %)))
                                                                      hand))
                                                  hand' (remove #{card} hand)
                                                  meta (if (and card (empty-hand? hand'))
                                                         {:last true}
                                                         {:last false})]
                                              (if card
                                                (a/>!! (players<->players i j) (with-meta card meta))
                                                (a/>!! (players<->players i j) (go)))
                                              (recur hand'))

                                        nil (do (doseq [card hand]
                                                  (a/>!! (dealer<->players i nil) card))
                                                (a/close! (dealer<->players i nil))
                                                (doseq [j opponent-ids]
                                                  (a/close! (players<->players i j))))))))))
              (range k))

        ;; Await termination
        output
        (do (a/<!! dealer)
            (doseq [i (range k)]
              (a/<!! (nth players i))))

        ;; Stop timer
        end (System/nanoTime)]

    (set! config/*output* output)
    (set! config/*time* (- end begin))))