(def range-without-i (fn [k i]
                       (remove #(= i %) (range k))))

(def deftypes (fn []
                (deftype Card [])
                (deftype Card? [])
                (deftype Card! [])
                (deftype Ack [])
                (deftype Turn [])
                (deftype Fish [])
                (deftype GoFish [])
                ))

(deftypes)

(def thread-dealer
  (fn [k dealer->players players->dealer barrier]
    (thread (doseq [_ (range 5)]
              (doseq [i (range k)]
                (>!! (nth dealer->players i) (->Card)))
              (doseq [i (range k)]
                (<!! (nth players->dealer i))))

            (>!! (nth dealer->players 0) (->Turn))

            (doseq [i (range k)]
              (thread
                (loop []
                  (<!! (nth players->dealer i))
                  (>!! (nth dealer->players i) (->Card))
                  (recur))))
            )))

(def thread-player
  (fn [i k dealer->players players->dealer players->players barrier]
    (thread (doseq [_ (range 5)]
              (<!! (nth dealer->players i))
              (>!! (nth players->dealer i) (->Ack)))

            (when (= i 0)
              (<!! (nth dealer->players 0))
              (>!! (aget players->players i (rand-nth (range-without-i k i))) (->Card?)))

            (doseq [j (range-without-i k i)]
              (thread
                (loop []
                  (let [msg (<!! (aget players->players j i))
                        _ (println j "-->" i ":" (type msg))]
                    (cond (= (type msg) Card?)
                          (do
                            (>!! (aget players->players i j) (if (= (rand-int 2) 0) (->Card!) (->GoFish))))
                          (or (= (type msg) Card!) (= (type msg) Turn))
                          (do
                            (>!! (aget players->players i (rand-nth (range-without-i k i))) (->Card?)))
                          (= (type msg) GoFish)
                          (do
                            (>!! (nth players->dealer i) (->Fish))
                            (<!! (nth dealer->players i))
                            (>!! (aget players->players i (mod (inc i) k)) (->Turn)))
                          :else
                          (throw (RuntimeException. (str (type msg)))))
                    (recur)))))
            )))