(ns discourje.examples.experimental.sg.discourje
  (require [discourje.examples.experimental.dsl :refer :all]
           [discourje.examples.experimental.api :refer :all]))

(defn alicefn
  [outs n]
  (doseq [_ (range n)]
    (doseq [out outs]
      (>!! out 0))))

(defn bobfn
  [i in n]
  (doseq [_ (range n)]
    (<!! in)))

(defn sg-discourje
  ([time k m]
   (sg-discourje time k 1000 m))
  ([time k n m]
   (let [chans (forv [i (range k)] (chan 1 (alice) (bob i) m))]
     (bench time #(let [a (thread (alicefn chans n))
                        bbb (forv [i (range k)] (thread (bobfn i (get chans i) n)))]
                    (join a) (join bbb) (monitor-reset m))))))

;(defn sg [k]
;  (fix :X (par (rep par [:i (range k)]
;                    (--> alice (bob :i) Long))
;               (fix :X))))

(defn sg [k]
  (par (rep par [:i (range k)]
            (--> (alice) (bob :i) Long))
       ))

(def K 2)

(monitor (spec (sg K)))

(sg-discourje 60 K 1 (monitor (spec (sg K))))