(ns discourje.examples.games.go-fish-types)

(defrecord Card [suit rank]
  Object
  (toString [_] (str suit rank)))

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

(defn shuffled-deck []
  (shuffle deck))

(defrecord Turn [])
(defn turn [] (->Turn))

(defrecord Ask [suit rank])
(defn ask [suit rank] (->Ask suit rank))

(defrecord Go [])
(defn go [] (->Go))

(defrecord Fish [])
(defn fish [] (->Fish))

(defrecord OutOfCards [])
(defn out-of-cards [] (->OutOfCards))