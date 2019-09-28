(ns discourje.examples.experimental.ring.clojure
  (require [clojure.core.async :refer [>!! <!! chan thread]]
           [discourje.examples.experimental.api :refer [bench join forv]]))

(defn alicefn
  [i in out n]
  (if (= i 0)
    ; alice 0
    (doseq [_ (range n)] (>!! out i) (<!! in))
    ; alice 1 <= i <= k
    (doseq [_ (range n)] (<!! in) (>!! out i))))

(defn ring-clojure
  ([time k]
   (ring-clojure time k 1000))
  ([time k n]
   (let [chans (forv [i (range k)] (chan 1))]
     (bench time #(let [aaa (forv [i (range k)]
                                  (thread (alicefn i
                                                   (get chans (mod (- i 1) k))
                                                   (get chans i)
                                                   n)))]
                    (join aaa))))))






(ring-clojure 60 1 2)