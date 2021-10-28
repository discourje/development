(ns discourje.examples.da.topologies)

(defn ring [n]
  (into (sorted-map)
        (map (fn [i]
               [i (sorted-set (mod (inc i) n)
                              (mod (dec i) n))])
             (range n))))

(defn star [n]
  (into (sorted-map)
        (map (fn [i]
               (if (= i 0)
                 [i (apply sorted-set (range 1 n))]
                 [i #{0}]))
             (range n))))

(defn tree [n]
  (into (sorted-map)
        (map (fn [i]
               (let [j1 (try (dec (Long/parseLong (.replaceFirst (Long/toBinaryString (inc i)) ".$" "") 2))
                             (catch Exception e -1))
                     j2 (dec (Long/parseLong (str (Long/toBinaryString (inc i)) "0") 2))
                     j3 (dec (Long/parseLong (str (Long/toBinaryString (inc i)) "1") 2))]
                 [i (apply sorted-set (filter #{j1 j2 j3} (range n)))]))
             (range n))))

(defn mesh-2d [n]
  (into (sorted-map)
        (map (fn [i]
               (let [len (long (Math/ceil (Math/sqrt n)))
                     row (long (/ i len))
                     col (mod i len)
                     j1 (if (> row 0) (clojure.core/+ (clojure.core/* (dec row) len) col) -1)
                     j2 (if (< row (dec len)) (clojure.core/+ (clojure.core/* (inc row) len) col) -1)
                     j3 (if (> col 0) (clojure.core/+ (clojure.core/* row len) (dec col)) -1)
                     j4 (if (< col (dec len)) (clojure.core/+ (clojure.core/* row len) (inc col)) -1)]
                 [i (apply sorted-set (filter (set (list j1 j2 j3 j4)) (range n)))]))
             (range n))))

(defn mesh-full [n]
  (into (sorted-map)
        (map (fn [i]
               [i (apply sorted-set (remove #{i} (range n)))])
             (range n))))