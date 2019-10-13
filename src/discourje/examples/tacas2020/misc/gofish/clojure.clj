(ns discourje.examples.tacas2020.misc.gofish.clojure
  (require [clojure.core.async :refer [>!! <!! close! chan thread]]))

;;
;; Implementation
;;

(load "threads")

(def run
  (fn [k]
    (let [dealer->players (vec (for [i (range k)] (chan 5)))
          players->dealer (vec (for [i (range k)] (chan 5)))
          players->players (to-array-2d (for [i (range k)]
                                          (for [j (range k)]
                                            (if (not= i j)
                                              (chan 1)
                                              nil))))]

      (thread-dealer k dealer->players players->dealer)
      (doseq [i (range k)]
        (thread-player i k dealer->players players->dealer players->players)))))

;(try
;  (run 4)
;  (catch Exception e (.printStackTrace e)))