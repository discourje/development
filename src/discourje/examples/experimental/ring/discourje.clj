(ns discourje.examples.experimental.ring.discourje
  (require [discourje.examples.experimental.dsl :refer :all]
           [discourje.examples.experimental.api :refer :all]))

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
   (let [chans (forv [i (range k)] (chan 1 (alice i) (alice (+ i 1)) m))]
     (bench time #(let [aaa (forv [i (range k)]
                                  (thread (alicefn i
                                                   (get chans (mod (- i 1) k))
                                                   (get chans i)
                                                   n)))]
                    (join aaa) (monitor-reset m))))))

(defn ring [k]
  (fix :X (seq (rep seq [:i (range k)]
                    (--> (alice :i) (alice (inc :i)) Long))
               (fix :X))))

(ring-discourje 60 2 1 (monitor (spec (ring 2))))