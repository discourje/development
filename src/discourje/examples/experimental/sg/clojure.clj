(ns discourje.examples.experimental.sg.clojure
  (require [clojure.core.async :refer [>!! <!! chan thread]]
           [discourje.examples.experimental.util :refer [bench join forv]]))

(defn alicefn
  [outs n]
  (doseq [_ (range n)]
    (doseq [out outs]
      (>!! out 0))))

(defn bobfn
  [i in n]
  (doseq [_ (range n)]
    (<!! in)))

(defn sg-clojure
  ([time k]
   (sg-clojure time k 1000))
  ([time k n]
   (let [chans (forv [i (range k)] (chan 1))]
     (bench time #(let [a (thread (alicefn chans n))
                        bbb (forv [i (range k)] (thread (bobfn i (get chans i) n)))]
                    (join a) (join bbb))))))

(def K 2)

(sg-clojure 60 K 1)