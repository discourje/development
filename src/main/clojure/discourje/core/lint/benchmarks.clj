(ns discourje.core.lint.benchmarks
  (:gen-class)
  (:require [clojure.test :refer :all]
            [clojure.pprint :refer :all]
            [discourje.core.spec :as s]
            [discourje.core.lint :as c]
            [discourje.core.spec.lts :as lts]
            [discourje.core.spec.mcrl2 :as mcrl2]))

(s/defrole :a)
(s/defrole :b)
(s/defrole :c)
(s/defrole :d)

(s/defsession :cheung [initiator network]
  (:cheung nil initiator {} network))

(s/defsession :cheung [i j parents network]
  (s/let [neighbours (get network j)
          todo (clojure.set/difference neighbours (set (keys parents)))]

         (s/if (not (contains? parents j))
           (:cheung i j (assoc parents j i) network)

           (s/if (not (empty? todo))
             (s/alt-every [k todo]
               (s/cat (s/--> (:a j) (:a k))
                      (:cheung j k parents network)))

             (s/let [k (get parents j)]
                    (s/if (some? k)
                      (s/cat (s/--> (:a j) (:a k))
                             (:cheung j k parents network))))))))

(s/defsession :awerbuch [initiator network]
  (:awerbuch nil initiator {} network))

(s/defsession :awerbuch [i j parents network]
  (s/let [neighbours (get network j)
          todo (clojure.set/difference neighbours (set (keys parents)))]

         (s/if (not (contains? parents j))

           (s/cat (s/par-every [k (remove #{i} neighbours)]
                    (s/cat (s/--> (:a j) (:a k))
                           (s/--> (:a k) (:a j))))
                  (:awerbuch i j (assoc parents j i) network))

           (s/if (not (empty? todo))
             (s/alt-every [k todo]
               (s/cat (s/--> (:a j) (:a k))
                      (:awerbuch j k parents network)))

             (s/let [k (get parents j)]
                    (s/if (some? k)
                      (s/cat (s/--> (:a j) (:a k))
                             (:awerbuch j k parents network))))))))

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

(defn -main [& args]
  (let [input (read-string (clojure.string/join " " args))
        algorithm (:algorithm input)
        initiator (:initiator input)
        network ((eval (read-string (str "discourje.core.lint.benchmarks/" (name (:network input))))) (:n input))
        spec (s/session algorithm [initiator network])

        begin-lts (System/nanoTime)
        lts (lts/lts spec)
        end-lts (System/nanoTime)
        time-lts (long (/ (- end-lts begin-lts) 1000000))

        begin-lint (System/nanoTime)
        results (binding [mcrl2/*mcrl2-bin* (str (:mcrl2-bin input))
                          mcrl2/*mcrl2-tmp* (str (:mcrl2-tmp input))]
                  (c/lint lts
                          :engine (:engine input)
                          :witness false
                          :exclude #{:send-before-close :causality}))
        end-lint (System/nanoTime)
        time-lint (long (/ (- end-lint begin-lint) 1000000))

        output (clojure.string/join " " [(:engine input)
                                         (:algorithm input)
                                         (:initiator input)
                                         (:network input)
                                         (:n input)
                                         (let [s (str lts)
                                               [_ transitions states] (.split (.substring
                                                                                s
                                                                                (inc (.indexOf s "("))
                                                                                (.indexOf s ")")) ",")]
                                           (str "(" states "," transitions ")"))
                                         time-lts
                                         time-lint])]
    (.println System/out results)
    (.println System/err output)
    (if (nil? (:no-exit input))
      (System/exit 0))))

(deftest -main-test
  (let [input {:engine    :dcj,
               :mcrl2-bin "/Applications/mCRL2.app/Contents/bin",
               :mcrl2-tmp "/Users/sungshik/Desktop/tmp",
               :algorithm :awerbuch,
               :initiator 0,
               :network   :ring,
               :n         4
               :no-exit   true}
        args (clojure.string/split (str input) #" ")]
    (apply -main args)))