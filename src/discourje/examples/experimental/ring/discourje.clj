(ns discourje.examples.experimental.ring.discourje
  (require [discourje.core.async :refer :all]
           [discourje.examples.experimental.api :refer :all]))

(def alice (role "alice"))
(def bob (role "bob"))

(defn alicefn
  [i in out n]
  (if (= i 0)
    ; alice 0
    (doseq [_ (range n)] (>!! out i) (<!! in))
    ; alice 1 <= i <= k
    (doseq [_ (range n)] (<!! in) (>!! out i))))

(defn ring-discourje
  ([time k m]
   (ring-discourje time k 1000 m))
  ([time k n m]
   (let [chans (forv [i (range k)] (chan 1 (alice i) (alice (inc i)) m))]
     (bench time #(let [aaa (forv [i (range k)]
                                  (thread (alicefn i
                                                   (get chans (mod (- i 1) k))
                                                   (get chans i)
                                                   n)))]
                    (join aaa))))))

(defn ring [k]
  (fix :X [(rep seq [i (range k)]
                (--> (alice i) (alice (inc i)) Long))
           (fix :X)]))

(ring-discourje 5 2 1 (mon (spec (ring 2))))